# Casino foundation implementation plan

Spec: ../specs/2026-09-26-casino-foundation-design.md
Execution: inline, approved by the user on 2026-09-26.

1. Text and languages: add MessageText rendering and Language.component/reload; regress literal parameters, legacy styles, invalid keys and transactional reload.
2. Menus: replace internal YAML with immutable MenuView; preserve single-use owner-bound sessions; regress expiration and stale tokens. Remove resource-font recovery graphics.
3. Runtime presentation: use Components at player boundaries; add permission-checked reload-language and refresh model labels without replacing geometry.
4. Displays: skip unchanged child traversal, update button pose only at press/release, measure operation counts. Improve visible-text label measurement without changing layout.
5. Audit storage and restoration; fix reproducible validation/lifetime defects. Retain payment and restore semantics. Evaluate indexing against measured scan costs.
6. Run Java/Python regressions and three-phase clean Purpur probe; produce text previews, alignment documentation, independent review, commit/push and verify CI.

Constraints: Casino only; Java 25 / Paper 26.2; no required plugins or client pack; no gameplay/payment changes; no Release publishing.
