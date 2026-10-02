"""
docs/privacy-policy-ko.md 를 웹에 그대로 올릴 수 있는 단일 HTML 파일로 바꾼다.

사용법 (저장소 루트에서):
    python scripts/build_privacy_policy_html.py
결과: docs/privacy-policy-ko.html (외부 파일·스크립트 없이 이 파일 하나만 올리면 된다)

방침 문서에 쓰는 문법만 처리한다: #/##/### 제목, 문단, - 목록, | 표 |, URL 자동 링크.
"""

import html
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/privacy-policy-ko.md"
OUTPUT = ROOT / "docs/privacy-policy-ko.html"

URL = re.compile(r"(https?://[^\s)<]+)")

STYLE = """
:root { --bg:#f4f3ef; --surface:#ffffff; --ink:#14213d; --ink-2:#4f586b; --line:#e2dfd7; --accent:#2f5bd3; }
@media (prefers-color-scheme: dark) {
  :root { --bg:#0e1422; --surface:#182033; --ink:#eef1f6; --ink-2:#a9b1c2; --line:#2a3450; --accent:#8fa9ff; }
}
* { box-sizing: border-box; }
body { margin:0; background:var(--bg); color:var(--ink);
  font-family:"IBM Plex Sans KR","Apple SD Gothic Neo","Noto Sans KR",system-ui,sans-serif;
  font-size:16px; line-height:1.7; word-break:keep-all; }
main { max-width:760px; margin:0 auto; padding:40px 20px 64px; }
h1 { font-size:28px; line-height:1.3; margin:0 0 8px; }
h2 { font-size:20px; margin:40px 0 12px; padding-top:20px; border-top:1px solid var(--line); }
h3 { font-size:17px; margin:24px 0 8px; }
p, li { color:var(--ink); }
.meta { color:var(--ink-2); margin:0 0 24px; }
ul { padding-left:20px; }
li { margin:4px 0; }
a { color:var(--accent); word-break:break-all; }
.table { overflow-x:auto; margin:12px 0; }
table { width:100%; border-collapse:collapse; background:var(--surface); font-size:15px; }
th, td { border:1px solid var(--line); padding:10px 12px; text-align:left; vertical-align:top; }
th { color:var(--ink-2); font-weight:600; white-space:nowrap; }
"""


def inline(text: str) -> str:
    escaped = html.escape(text)
    return URL.sub(lambda m: f'<a href="{m.group(1)}" rel="noopener">{m.group(1)}</a>', escaped)


def convert(markdown: str) -> tuple[str, str]:
    lines = markdown.splitlines()
    out: list[str] = []
    title = "개인정보처리방침"
    i = 0
    while i < len(lines):
        line = lines[i].rstrip()
        if not line:
            i += 1
            continue
        if line.startswith("# "):
            title = line[2:].strip()
            out.append(f"<h1>{inline(title)}</h1>")
        elif line.startswith("### "):
            out.append(f"<h3>{inline(line[4:])}</h3>")
        elif line.startswith("## "):
            out.append(f"<h2>{inline(line[3:])}</h2>")
        elif line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            header, body = rows[0], [r for r in rows[1:] if not set("".join(r)) <= set("-: ")]
            out.append('<div class="table"><table><thead><tr>')
            out.append("".join(f"<th>{inline(c)}</th>" for c in header))
            out.append("</tr></thead><tbody>")
            for row in body:
                out.append("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in row) + "</tr>")
            out.append("</tbody></table></div>")
            continue
        elif line.startswith("- "):
            out.append("<ul>")
            while i < len(lines) and lines[i].startswith("- "):
                out.append(f"<li>{inline(lines[i][2:])}</li>")
                i += 1
            out.append("</ul>")
            continue
        elif line.startswith("시행일:"):
            out.append(f'<p class="meta">{inline(line)}</p>')
        else:
            out.append(f"<p>{inline(line)}</p>")
        i += 1
    return title, "\n".join(out)


def main() -> None:
    title, body = convert(SOURCE.read_text(encoding="utf-8"))
    page = f"""<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<style>{STYLE}</style>
</head>
<body>
<main>
{body}
</main>
</body>
</html>
"""
    OUTPUT.write_text(page, encoding="utf-8", newline="\n")
    print(OUTPUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
