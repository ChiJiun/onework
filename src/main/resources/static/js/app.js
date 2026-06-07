const state = {
    token: localStorage.getItem("token"),
    user: JSON.parse(localStorage.getItem("user") || "null"),
    rooms: [],
    rejectId: null
};

const $ = id => document.getElementById(id);
const pages = ["overview", "booking", "timeline", "monthly", "review"];
const titles = { overview: "預約總覽", booking: "預約會議室", timeline: "每日行程", monthly: "每月預約狀況", review: "預約審核" };

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    setDateDefaults();
    if (state.token && state.user) enterApp();
});

function bindEvents() {
    $("loginForm").addEventListener("submit", login);
    $("logoutBtn").addEventListener("click", logout);
    $("bookingForm").addEventListener("submit", createBooking);
    $("loadTimeline").addEventListener("click", loadTimeline);
    $("loadMonthly").addEventListener("click", loadMonthly);
    $("rejectForm").addEventListener("submit", event => {
        event.preventDefault();
        rejectReservation();
    });
    document.querySelectorAll("[data-account]").forEach(button => button.addEventListener("click", () => {
        $("username").value = button.dataset.account;
        $("password").value = "password";
    }));
    document.querySelectorAll("[data-page], [data-go]").forEach(button => button.addEventListener("click", () => {
        showPage(button.dataset.page || button.dataset.go);
    }));
}

async function api(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    const response = await fetch(path, { ...options, headers });
    if (response.status === 401 || response.status === 403) {
        if (path !== "/api/auth/login") logout();
        throw new Error("登入已失效或權限不足");
    }
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        const validation = body?.validationErrors ? Object.values(body.validationErrors).join("、") : "";
        throw new Error(validation || body?.message || `請求失敗 (${response.status})`);
    }
    return body;
}

async function login(event) {
    event.preventDefault();
    $("loginError").textContent = "";
    try {
        const result = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ username: $("username").value, password: $("password").value })
        });
        state.token = result.token;
        state.user = { id: result.userId, name: result.displayName, role: result.role };
        localStorage.setItem("token", state.token);
        localStorage.setItem("user", JSON.stringify(state.user));
        await enterApp();
    } catch (error) {
        $("loginError").textContent = error.message === "請求失敗 (401)" ? "帳號或密碼錯誤" : error.message;
    }
}

async function enterApp() {
    $("loginView").classList.add("hidden");
    $("appView").classList.remove("hidden");
    $("userName").textContent = state.user.name;
    $("userRole").textContent = state.user.role;
    $("avatar").textContent = state.user.name.charAt(0).toUpperCase();
    const canReview = ["REVIEWER", "ADMIN"].includes(state.user.role);
    $("reviewNav").classList.toggle("hidden", !canReview);
    try {
        await loadRooms();
        await loadDashboard();
        showPage("overview");
    } catch (error) {
        toast(error.message, true);
    }
}

function logout() {
    localStorage.clear();
    state.token = null;
    state.user = null;
    location.reload();
}

function showPage(name) {
    if (name === "review" && !["REVIEWER", "ADMIN"].includes(state.user.role)) return;
    pages.forEach(page => $(`${page}Page`).classList.toggle("hidden", page !== name));
    document.querySelectorAll(".nav-item").forEach(item => item.classList.toggle("active", item.dataset.page === name));
    $("pageTitle").textContent = titles[name];
    if (name === "timeline") loadTimeline();
    if (name === "monthly") loadMonthly();
    if (name === "review") loadReviews();
}

async function loadRooms() {
    state.rooms = await api("/api/rooms");
    $("roomCount").textContent = state.rooms.length;
    $("bookingRoom").innerHTML = state.rooms.map(room =>
        `<option value="${room.id}">${escapeHtml(room.name)}（${room.capacity} 人）</option>`
    ).join("");
    $("roomGrid").innerHTML = state.rooms.map(room => `
        <article class="room-card">
            <span class="capacity">${room.capacity} 人 · ${escapeHtml(room.location || "未設定樓層")}</span>
            <h3>${escapeHtml(room.name)}</h3>
            <p>${escapeHtml(room.equipment || "一般會議設備")}</p>
            <button onclick="selectRoom(${room.id})">預約此空間</button>
        </article>`).join("");
}

async function loadDashboard() {
    const today = localDate();
    const month = today.slice(0, 7);
    const [timeline, monthly] = await Promise.all([
        api(`/api/reservations/timeline?date=${today}`),
        api(`/api/reservations/monthly?month=${month}`)
    ]);
    $("todayCount").textContent = timeline.reservations.length;
    $("pendingCount").textContent = monthly.counts.PROCESSING || 0;
}

window.selectRoom = id => {
    $("bookingRoom").value = String(id);
    showPage("booking");
};

async function createBooking(event) {
    event.preventDefault();
    try {
        await api("/api/reservations", {
            method: "POST",
            body: JSON.stringify({
                roomId: Number($("bookingRoom").value),
                userId: state.user.id,
                title: $("bookingTitle").value,
                description: $("bookingDescription").value || null,
                attendeeCount: Number($("attendeeCount").value),
                startTime: $("startTime").value,
                endTime: $("endTime").value
            })
        });
        toast("預約已送出，等待審核");
        $("bookingForm").reset();
        setDateDefaults();
        await loadDashboard();
        showPage("overview");
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadTimeline() {
    try {
        const data = await api(`/api/reservations/timeline?date=${$("timelineDate").value}`);
        $("timelineSummary").textContent = `${data.reservedRoomCount} 間會議室 · ${data.reservations.length} 場會議`;
        $("timelineList").innerHTML = data.reservations.length ? data.reservations.map(item => `
            <div class="timeline-item">
                <div class="timeline-time">${time(item.startTime)}<br><span class="muted">${time(item.endTime)}</span></div>
                <div class="timeline-line"></div>
                <div class="timeline-content">
                    <strong>${escapeHtml(item.title)}</strong>
                    <p>${escapeHtml(item.roomName)} · ${escapeHtml(item.requesterName)} · ${item.attendeeCount} 人</p>
                    <small>${escapeHtml(item.description || "無備註")}</small>
                </div>
            </div>`).join("") : empty("當日尚無已核准的預約");
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadMonthly() {
    try {
        const status = $("statusFilter").value;
        const data = await api(`/api/reservations/monthly?month=${$("monthInput").value}${status ? `&status=${status}` : ""}`);
        $("monthlyStats").innerHTML = ["PROCESSING", "APPROVED", "REJECTED"].map(value =>
            `<article><span>${statusLabel(value)}</span><strong>${data.counts[value] || 0}</strong><small>筆預約</small></article>`
        ).join("");
        $("monthlyRows").innerHTML = data.reservations.length ? data.reservations.map(item => `
            <tr><td>${formatDateTime(item.startTime)}</td><td>${escapeHtml(item.roomName)}</td><td>${escapeHtml(item.title)}</td>
            <td>${escapeHtml(item.requesterName)}</td><td><span class="badge ${item.status}">${statusLabel(item.status)}</span></td></tr>`
        ).join("") : `<tr><td colspan="5" class="empty">此月份沒有符合條件的預約</td></tr>`;
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadReviews() {
    try {
        const data = await api(`/api/reservations/monthly?month=${$("monthInput").value}&status=PROCESSING`);
        $("reviewList").innerHTML = data.reservations.length ? data.reservations.map(item => `
            <article class="review-card">
                <div><h3>${escapeHtml(item.title)}</h3>
                    <p>${formatDateTime(item.startTime)} - ${time(item.endTime)} · ${escapeHtml(item.roomName)}</p>
                    <p>申請人：${escapeHtml(item.requesterName)} · ${item.attendeeCount} 人</p>
                    <p>${escapeHtml(item.description || "無備註")}</p>
                </div>
                <div class="review-actions">
                    <button class="secondary" onclick="openReject(${item.id})">退回</button>
                    <button class="primary" onclick="approve(${item.id})">通過</button>
                </div>
            </article>`).join("") : empty("目前沒有待審核的預約");
    } catch (error) {
        toast(error.message, true);
    }
}

window.approve = async id => {
    try {
        await review(id, "APPROVED");
        toast("預約已通過");
        await loadReviews();
    } catch (error) { toast(error.message, true); }
};

window.openReject = id => {
    state.rejectId = id;
    $("rejectReason").value = "";
    $("rejectDialog").showModal();
};

async function rejectReservation() {
    try {
        await review(state.rejectId, "REJECTED", $("rejectReason").value);
        $("rejectDialog").close();
        toast("預約已退回");
        await loadReviews();
    } catch (error) { toast(error.message, true); }
}

function review(id, status, reason = null) {
    return api(`/api/reservations/${id}/review`, {
        method: "PATCH",
        body: JSON.stringify({ reviewerId: state.user.id, status, reason })
    });
}

function setDateDefaults() {
    const now = new Date();
    $("timelineDate").value = localDate(now);
    $("monthInput").value = localDate(now).slice(0, 7);
    const start = new Date(now.getTime() + 24 * 60 * 60 * 1000);
    start.setMinutes(0, 0, 0);
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    $("startTime").value = localDateTime(start);
    $("endTime").value = localDateTime(end);
}

function localDate(date = new Date()) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}
function localDateTime(date) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
function time(value) { return value?.slice(11, 16) || ""; }
function formatDateTime(value) { return `${value.slice(0,10)} ${time(value)}`; }
function statusLabel(value) { return ({ PROCESSING: "待審核", APPROVED: "已通過", REJECTED: "已退回" })[value] || value; }
function empty(message) { return `<div class="empty">${message}</div>`; }
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" })[char]);
}
function toast(message, isError = false) {
    const element = $("toast");
    element.textContent = message;
    element.className = `toast show${isError ? " error" : ""}`;
    setTimeout(() => element.className = "toast", 3000);
}
