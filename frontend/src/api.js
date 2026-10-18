let session;
try {
  session = JSON.parse(sessionStorage.getItem("session") || "null");
} catch {
  session = null;
}
export const getSession = () => session;
export function saveSession(value) {
  session = value;
  if (value) sessionStorage.setItem("session", JSON.stringify(value));
  else sessionStorage.removeItem("session");
}
let refreshing;
export async function api(path, method = "GET", data, retry = true) {
  if(path === "/meter-readings" && method === "POST" && data?.settlement) path="/meter-readings/settlement";
  const originalSession = session;
  const response = await fetch("/api" + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(session ? { Authorization: "Bearer " + session.accessToken } : {}),
    },
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  if (
    response.status === 401 &&
    session &&
    originalSession === session &&
    retry &&
    !path.startsWith("/auth/")
  ) {
    try {
      refreshing ??= api(
        "/auth/refresh",
        "POST",
        { refreshToken: session.refreshToken },
        false,
      )
        .then((value) => {
          if (session !== originalSession) throw new Error("Session changed");
          saveSession(value);
        })
        .finally(() => (refreshing = null));
      await refreshing;
      return api(path, method, data, false);
    } catch {
      if (session === originalSession) {
        saveSession(null);
        window.dispatchEvent(new Event("session-expired"));
      }
      throw new Error("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
    }
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(
      error.message || "Không thể xử lý yêu cầu (" + response.status + ").",
    );
  }
  if (response.status === 204) return null;
  return response.json();
}
export async function download(path, name) {
  const response = await fetch("/api" + path, {
    headers: { Authorization: "Bearer " + getSession().accessToken },
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || "Không thể tải tệp.");
  }
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export async function upload(path, file) {
  // Refresh first so a multipart request does not need to be replayed.
  await api("/auth/me");
  const body = new FormData(); body.append("file", file);
  const response = await fetch("/api" + path, {
    method: "POST", headers: { Authorization: "Bearer " + getSession().accessToken }, body,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || "Không thể tải ảnh lên.");
  }
  return response.json();
}
export async function imageBlob(path) {
  await api("/auth/me");
  const response = await fetch(path, {headers:{Authorization:"Bearer "+getSession().accessToken}});
  if (!response.ok) throw new Error("Không thể tải ảnh.");
  return URL.createObjectURL(await response.blob());
}
