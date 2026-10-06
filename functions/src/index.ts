import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";

const NEST_CLIENT_ID = defineSecret("NEST_CLIENT_ID");
const NEST_CLIENT_SECRET = defineSecret("NEST_CLIENT_SECRET");
const NEST_REFRESH_TOKEN = defineSecret("NEST_REFRESH_TOKEN");

const DEVICE_ACCESS_PROJECT_ID = "9ae59f3c-8dbc-4551-9190-35eb9fe39db7";
const REGION = "europe-west1";
const SDM_BASE = "https://smartdevicemanagement.googleapis.com/v1";

async function accessToken(): Promise<string> {
  const body = new URLSearchParams({
    client_id: NEST_CLIENT_ID.value(),
    client_secret: NEST_CLIENT_SECRET.value(),
    refresh_token: NEST_REFRESH_TOKEN.value(),
    grant_type: "refresh_token",
  });

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {"Content-Type": "application/x-www-form-urlencoded"},
    body,
  });

  const json = await response.json() as Record<string, unknown>;
  if (!response.ok) {
    const description = String(json.error_description ?? json.error ?? "OAuth refresh failed");
    throw new HttpsError("unauthenticated", description);
  }

  const token = json.access_token;
  if (typeof token !== "string" || token.length === 0) {
    throw new HttpsError("internal", "Google returned no access token.");
  }
  return token;
}

function requireUser(auth: unknown): void {
  if (!auth) throw new HttpsError("unauthenticated", "Firebase login required.");
}

function deviceName(value: unknown): string {
  if (typeof value !== "string" ||
      !value.startsWith("enterprises/" + DEVICE_ACCESS_PROJECT_ID + "/devices/")) {
    throw new HttpsError("invalid-argument", "Invalid Doorbell device name.");
  }
  return value;
}

async function sdmJson(
  url: string,
  options: RequestInit = {},
): Promise<Record<string, unknown>> {
  const token = await accessToken();
  const response = await fetch(url, {
    ...options,
    headers: {
      "Authorization": "Bearer " + token,
      "Accept": "application/json",
      ...(options.body ? {"Content-Type": "application/json"} : {}),
      ...(options.headers ?? {}),
    },
  });

  const text = await response.text();
  const json = text ? JSON.parse(text) as Record<string, unknown> : {};
  if (!response.ok) {
    const error = json.error as Record<string, unknown> | undefined;
    throw new HttpsError(
      "failed-precondition",
      String(error?.message ?? ("Google SDM HTTP " + response.status)),
    );
  }
  return json;
}

async function command(
  name: string,
  commandName: string,
  params: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return sdmJson(SDM_BASE + "/" + name + ":executeCommand", {
    method: "POST",
    body: JSON.stringify({
      command: commandName,
      params,
    }),
  });
}

export const nestListDoorbells = onCall(
  {region: REGION, secrets: [NEST_CLIENT_ID, NEST_CLIENT_SECRET, NEST_REFRESH_TOKEN]},
  async (request) => {
    requireUser(request.auth);
    const result = await sdmJson(
      SDM_BASE + "/enterprises/" + DEVICE_ACCESS_PROJECT_ID + "/devices",
    );
    const devices = Array.isArray(result.devices) ? result.devices : [];

    return {
      doorbells: devices
        .filter((raw): raw is Record<string, unknown> =>
          typeof raw === "object" &&
          raw !== null &&
          raw.type === "sdm.devices.types.DOORBELL")
        .map((raw) => {
          const traits = (raw.traits ?? {}) as Record<string, unknown>;
          const live = (traits["sdm.devices.traits.CameraLiveStream"] ?? {}) as Record<string, unknown>;
          const protocols = Array.isArray(live.supportedProtocols) ? live.supportedProtocols : [];
          const info = (traits["sdm.devices.traits.Info"] ?? {}) as Record<string, unknown>;
          return {
            id: String(raw.name ?? ""),
            name: String(info.customName ?? "Google Doorbell"),
            supportsWebRtc: protocols.includes("WEB_RTC"),
          };
        }),
    };
  },
);

export const nestCreateWebRtcSession = onCall(
  {region: REGION, secrets: [NEST_CLIENT_ID, NEST_CLIENT_SECRET, NEST_REFRESH_TOKEN]},
  async (request) => {
    requireUser(request.auth);
    const name = deviceName(request.data?.deviceName);
    const offerSdp = request.data?.offerSdp;
    if (typeof offerSdp !== "string" || offerSdp.length < 20 || offerSdp.length > 100_000) {
      throw new HttpsError("invalid-argument", "Invalid WebRTC offer.");
    }

    const result = await command(
      name,
      "sdm.devices.commands.CameraLiveStream.GenerateWebRtcStream",
      {offerSdp},
    );
    return result.results ?? {};
  },
);

export const nestExtendWebRtcSession = onCall(
  {region: REGION, secrets: [NEST_CLIENT_ID, NEST_CLIENT_SECRET, NEST_REFRESH_TOKEN]},
  async (request) => {
    requireUser(request.auth);
    const name = deviceName(request.data?.deviceName);
    const mediaSessionId = request.data?.mediaSessionId;
    if (typeof mediaSessionId !== "string" || mediaSessionId.length === 0) {
      throw new HttpsError("invalid-argument", "Missing media session.");
    }

    const result = await command(
      name,
      "sdm.devices.commands.CameraLiveStream.ExtendWebRtcStream",
      {mediaSessionId},
    );
    return result.results ?? {};
  },
);

export const nestStopWebRtcSession = onCall(
  {region: REGION, secrets: [NEST_CLIENT_ID, NEST_CLIENT_SECRET, NEST_REFRESH_TOKEN]},
  async (request) => {
    requireUser(request.auth);
    const name = deviceName(request.data?.deviceName);
    const mediaSessionId = request.data?.mediaSessionId;
    if (typeof mediaSessionId !== "string" || mediaSessionId.length === 0) {
      throw new HttpsError("invalid-argument", "Missing media session.");
    }

    await command(
      name,
      "sdm.devices.commands.CameraLiveStream.StopWebRtcStream",
      {mediaSessionId},
    );
    return {ok: true};
  },
);
