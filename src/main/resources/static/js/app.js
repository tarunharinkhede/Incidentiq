const $ = (s, r = document) => r.querySelector(s);
const esc = (s) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (m) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        m
      ],
  );
const sc = (s) =>
  ({ CRITICAL: "cr", HIGH: "hg", MEDIUM: "md", LOW: "lo" })[
    (s || "").toUpperCase()
  ] || "nn";
const cap = (s) =>
  s ? s[0].toUpperCase() + s.slice(1).toLowerCase() : "Not rated";
const ago = (t) => {
  if (!t) return "";
  const m = (Date.now() - new Date(t)) / 60000;
  if (m < 1) return "just now";
  if (m < 60) return `${Math.floor(m)} min ago`;
  const h = m / 60;
  return h < 24 ? `${Math.floor(h)} h ago` : `${Math.floor(h / 24)} d ago`;
};

async function req(path, opt = {}) {
  const r = await fetch("/api" + path, {
    headers: { "Content-Type": "application/json" },
    ...opt,
  });
  const d = await r.json().catch(() => ({}));
  if (!r.ok)
    throw new Error(
      d.fieldErrors
        ? Object.values(d.fieldErrors).join(", ")
        : d.message || "Request failed",
    );
  return d;
}
function toast(m, t = "") {
  const e = document.createElement("div");
  e.className = "toast " + t;
  e.textContent = m;
  $("#toasts").append(e);
  setTimeout(() => e.remove(), 4800);
}

/* ---------- shared pieces ---------- */
const pill = (s) => `<span class="pill ${sc(s)}"><i></i>${cap(s)}</span>`;
const blk = (h, t) =>
  t ? `<div class="blk"><h4>${h}</h4><p>${esc(t)}</p></div>` : "";
const row = (
  i,
) => `<a class="row ${sc(i.severity)}" href="#/incident/${i.id}"><span class="dot"></span>
  <div><b>${esc(i.title)}</b><small>${esc(i.service)}${i.category ? ", " + esc(i.category) : ""}</small></div>
  <div class="r"><span class="chip ${i.status === "RESOLVED" ? "ok" : ""}">${i.status === "RESOLVED" ? "Resolved" : "Open"}</span><small>${ago(i.createdAt)}</small></div></a>`;
const steps = (n) =>
  `<section class="panel"><ul class="steps">${[
    "Saving the incident",
    "Checking past incidents, then asking the AI if the error is new",
  ]
    .map(
      (t, k) =>
        `<li class="${k + 1 < n ? "done" : k + 1 === n ? "now" : ""}"><i></i>${t}</li>`,
    )
    .join("")}</ul></section>`;
const emptyVerdict =
  () => `<section class="panel empty"><h2>Your analysis appears here</h2>
  <p>We search past incidents first. The AI is only called when the error is new, which saves time and API cost.</p></section>`;

function verdict(i) {
  const m = i.matchedIncidentId,
    pct = Math.round((i.similarityScore || 0) * 100);
  const src = m
    ? `<div class="seen"><div class="ring" style="--p:${pct}"><b>${pct}%</b></div><div><h3>Seen before</h3>
       <p>Matches <a href="#/incident/${m}">incident #${m}</a>. Its fix was reused, so no AI call was made.</p></div></div>`
    : `<div class="seen fresh"><div><h3>New error</h3><p>No close match in past incidents, so the AI analyzed it.</p></div></div>`;
  return `<section class="panel">${src}<div class="vh">${pill(i.severity)}<span class="muted">${esc(i.category || "")}</span></div>
    <p class="sum">${esc(i.summary)}</p>${blk("Root cause", i.rootCause)}${blk("Suggested fix", i.suggestion)}
    ${
      i.checklist?.length
        ? `<div class="blk"><h4>Checklist</h4></div><ul class="checks">${i.checklist
            .map(
              (c) =>
                `<li><label><input type="checkbox"><span>${esc(c)}</span></label></li>`,
            )
            .join("")}</ul>`
        : ""
    }</section>`;
}

/* ---------- views ---------- */
function viewAnalyze(v) {
  v.innerHTML = `<div class="page"><h1>Analyze an incident</h1>
    <p class="lede">Paste the logs from the crash. We check past incidents first and only ask the AI when the error is new.</p>
    <div class="split"><form id="f" class="panel">
      <label>Title<input name="title" placeholder="Database connection failure" required></label>
      <label>Service<input name="service" placeholder="user-service" required></label>
      <label>Logs<div class="logbox" id="lb"><textarea name="log" spellcheck="false" required placeholder="Paste the stack trace or terminal output here"></textarea></div></label>
      <button class="btn" id="go">Analyze logs</button></form>
      <div id="out" aria-live="polite">${emptyVerdict()}</div></div></div>`;
  $("#f").onsubmit = async (e) => {
    e.preventDefault();
    const body = Object.fromEntries(new FormData(e.target)),
      btn = $("#go"),
      out = $("#out");
    let id = null;
    btn.disabled = true;
    btn.textContent = "Analyzing…";
    $("#lb").classList.add("scan");
    out.innerHTML = steps(1);
    try {
      id = (
        await req("/incidents", { method: "POST", body: JSON.stringify(body) })
      ).id;
      out.innerHTML = steps(2);
      const inc = await req(`/incidents/${id}/analyze`, { method: "POST" });
      out.innerHTML =
        verdict(inc) +
        `<p><a class="btn ghost" href="#/incident/${inc.id}">Open incident #${inc.id}</a></p>`;
    } catch (err) {
      out.innerHTML = emptyVerdict();
      toast(
        id
          ? `Saved as incident #${id}, but analysis failed: ${err.message}`
          : err.message,
        "err",
      );
    } finally {
      btn.disabled = false;
      btn.textContent = "Analyze logs";
      $("#lb").classList.remove("scan");
    }
  };
}

async function viewDashboard(v) {
  const [d, all] = await Promise.all([req("/dashboard"), req("/incidents")]);
  if (!d.total) {
    v.innerHTML = `<div class="page"><div class="empty"><h2>No incidents yet</h2>
    <p>Paste your first log and it becomes the start of your knowledge base.</p><a class="btn ghost" href="#/analyze">Analyze a log</a></div></div>`;
    return;
  }
  const pct = Math.round((d.resolved / d.total) * 100);
  const cats = Object.entries(d.byCategory || {}).sort((a, b) => b[1] - a[1]),
    max = cats.length ? cats[0][1] : 1;
  const sev = [
    ["CRITICAL", "cr", "Critical"],
    ["HIGH", "hg", "High"],
    ["MEDIUM", "md", "Medium"],
    ["LOW", "lo", "Low"],
  ].map(([k, c, l]) => [c, l, (d.bySeverity || {})[k] || 0]);
  v.innerHTML = `<div class="page"><h1>${d.resolved} of ${d.total} incidents resolved</h1>
    <div class="prog" role="progressbar" aria-valuenow="${pct}" aria-valuemin="0" aria-valuemax="100"><i style="--w:${pct}%"></i></div>
    <p class="facts"><b>${d.open}</b> still open, <b>${d.critical}</b> critical${cats[0] ? `, and <b>${esc(cats[0][0])}</b> is the most common category` : ""}.</p>
    <div class="cols"><section><h3>Where incidents come from</h3><div class="bars">${
      cats
        .map(
          ([n, c]) =>
            `<div class="b"><span>${esc(n)}</span><div class="t"><i style="--w:${(c / max) * 100}%"></i></div><b>${c}</b></div>`,
        )
        .join("") ||
      '<p class="muted">Categories appear after the first analysis.</p>'
    }</div></section>
    <section><h3>How serious they are</h3><div class="stack">${sev
      .filter((s) => s[2])
      .map(([c, , n]) => `<i class="${c}" style="--n:${n}"></i>`)
      .join("")}</div>
      <ul class="lg">${sev.map(([c, l, n]) => `<li class="${c}"><span><i></i>${l}</span><b>${n}</b></li>`).join("")}</ul></section></div>
    <h3>Latest incidents</h3><div class="rows">${all.slice(-5).reverse().map(row).join("")}</div></div>`;
}

async function viewIncidents(v) {
  const all = (await req("/incidents")).reverse();
  let q = "",
    st = "all";
  v.innerHTML = `<div class="page"><h1>Incidents</h1><p class="lede">Every incident you have analyzed. Resolved ones become the answers we reuse next time.</p>
    <div class="bar"><input id="q" type="search" placeholder="Search by title, service or category" aria-label="Search incidents">
    <div class="seg" id="seg">${["all", "open", "resolved"].map((s) => `<button type="button" data-s="${s}" class="${s === "all" ? "on" : ""}">${cap(s)}</button>`).join("")}</div></div>
    <div class="rows" id="rows"></div></div>`;
  const draw = () => {
    const l = all.filter(
      (i) =>
        (st === "all" || (st === "resolved") === (i.status === "RESOLVED")) &&
        `${i.title} ${i.service} ${i.category || ""}`.toLowerCase().includes(q),
    );
    $("#rows").innerHTML = l.length
      ? l.map(row).join("")
      : `<div class="empty" style="padding:40px 0"><h2>${all.length ? "Nothing matches" : "No incidents yet"}</h2>
      <p>${all.length ? "Try a different search or filter." : "Analyze a log to create the first one."}</p></div>`;
  };
  $("#q").oninput = (e) => {
    q = e.target.value.toLowerCase().trim();
    draw();
  };
  $("#seg").onclick = (e) => {
    const b = e.target.closest("button");
    if (!b) return;
    st = b.dataset.s;
    document
      .querySelectorAll("#seg button")
      .forEach((x) => x.classList.toggle("on", x === b));
    draw();
  };
  draw();
}

async function viewIncident(v, id) {
  const i = await req("/incidents/" + id),
    res = i.status === "RESOLVED";
  const lines = (i.log || "")
    .split("\n")
    .map(
      (l) =>
        `<span class="${/error|exception|caused by|fatal/i.test(l) ? "bad" : ""}">${esc(l) || " "}</span>`,
    )
    .join("");
  v.innerHTML = `<div class="page"><a class="back" href="#/incidents">Back to incidents</a><h1>${esc(i.title)}</h1>
    <p class="meta"><span>${esc(i.service)}</span><span>Reported ${ago(i.createdAt)}</span><span class="chip ${res ? "ok" : ""}">${res ? "Resolved" : "Open"}</span></p>
    <div class="split"><div><h3>Original log</h3><pre class="log">${lines}</pre></div><div class="stackv">
    ${i.summary ? verdict(i) : `<section class="panel"><h3>Not analyzed yet</h3><p class="muted">Run the analysis to get a summary, root cause and fix.</p><button class="btn" id="an">Analyze now</button></section>`}
    ${
      res
        ? `<section class="panel"><h3>Resolved by ${esc(i.resolvedBy || "unknown")}</h3><p class="muted">${new Date(i.resolvedAt).toLocaleString()}</p><p>${esc(i.resolutionNotes)}</p></section>`
        : `<form class="panel" id="rf"><h3>Resolve this incident</h3>
        <label>What fixed it?<textarea class="sm" name="resolutionNotes" required placeholder="Changed the DB password and restarted MySQL"></textarea></label>
        <label>Your name<input name="resolvedBy" placeholder="Tarun"></label><button class="btn">Mark resolved</button></form>`
    }
    </div></div></div>`;
  const an = $("#an");
  if (an)
    an.onclick = async () => {
      an.disabled = true;
      an.textContent = "Analyzing…";
      try {
        await req(`/incidents/${id}/analyze`, { method: "POST" });
        toast("Analysis complete");
        viewIncident(v, id);
      } catch (e) {
        toast(e.message, "err");
        an.disabled = false;
        an.textContent = "Analyze now";
      }
    };
  const rf = $("#rf");
  if (rf)
    rf.onsubmit = async (e) => {
      e.preventDefault();
      try {
        await req(`/incidents/${id}/resolve`, {
          method: "PUT",
          body: JSON.stringify(Object.fromEntries(new FormData(rf))),
        });
        toast("Marked resolved");
        viewIncident(v, id);
      } catch (err) {
        toast(err.message, "err");
      }
    };
}

/* ---------- router and theme ---------- */
const routes = {
  analyze: viewAnalyze,
  dashboard: viewDashboard,
  incidents: viewIncidents,
  incident: viewIncident,
};
async function route() {
  const [name = "analyze", arg] = location.hash.replace(/^#\//, "").split("/");
  document
    .querySelectorAll(".rail nav a")
    .forEach((a) =>
      a.classList.toggle(
        "on",
        a.dataset.r === (name === "incident" ? "incidents" : name),
      ),
    );
  const v = $("#view");
  v.innerHTML = '<div class="page"><p class="muted">Loading…</p></div>';
  try {
    await (routes[name] || viewAnalyze)(v, arg);
    window.scrollTo(0, 0);
  } catch (e) {
    v.innerHTML = `<div class="page"><div class="empty"><h2>Couldn't load this page</h2><p>${esc(e.message)}</p></div></div>`;
  }
}
const tb = $("#theme");
const setTheme = (t) => {
  document.documentElement.dataset.theme = t;
  try {
    localStorage.setItem("iq-theme", t);
  } catch (e) {}
  tb.textContent = t === "dark" ? "Switch to light" : "Switch to dark";
};
tb.onclick = () =>
  setTheme(
    document.documentElement.dataset.theme === "dark" ? "light" : "dark",
  );
setTheme(document.documentElement.dataset.theme);
addEventListener("hashchange", route);
route();
