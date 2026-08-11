# Archive login repeated-attempt defense decision

Decision date: 2026-08-10.

## Decision

Keep the existing bounded in-memory limiter for the current single-node,
direct-Tomcat internal service. No login behavior was changed in this security
patch.

Current contract:

- Account key: 5 failures in 5 minutes, then a 5-minute block.
- Client-address key: 30 failures in 5 minutes, then a 5-minute block.
- At most 10,000 keys are retained; expired keys are removed and overflow is
  bounded.
- Account IDs are normalized and truncated; raw passwords are never retained.
- A successful login clears the account state, not the client-wide state.
- Blocked and unknown-account responses do not reveal whether the account
  exists. A blocked response includes only `Retry-After`.
- The clock and cache parameters are injectable for deterministic tests.

## Options considered

| Option | Fit now | Trade-off |
| --- | --- | --- |
| Reverse-proxy rate limit | Not currently available | Strong edge protection, but no reviewed Archive proxy/rate-limit service is deployed |
| Bounded in-memory limiter | Selected | Smallest change and deterministic; state is per JVM and resets on restart |
| Central authentication/SSO | Deferred | Best central policy/audit, but introduces an external identity system and migration |

## Client-IP trust boundary

`LoginServlet` uses `request.getRemoteAddr()` and does not trust forwarding
headers. This is correct while clients connect directly to Tomcat. If a proxy
is introduced, client identity must be established by a tightly scoped Tomcat
`RemoteIpValve` and an edge that strips attacker-supplied forwarding headers.
Do not add application-side fallback trust for `X-Forwarded-For`.

## Known limitations and next trigger

- Multiple application nodes would have independent limits.
- A restart clears transient limiter state.
- Host/network-level flooding still needs an edge or firewall control.

Re-evaluate the decision before adding a second Tomcat node, exposing Archive
outside the internal network, or adopting SSO. Those are product/operations
changes and require separate approval.
