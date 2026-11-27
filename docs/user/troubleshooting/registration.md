# Connecting an Agent to a Central Server

An agent sends its quality reports to a central server. Before it can do that, two things need to happen:

1. **The agent registers.** In the agent, go to **Central Servers**, click **Add Central Server**, enter a
   name and the server's address, and click **Register Server**. The agent then introduces itself to the server.
2. **An administrator approves it.** The agent appears on the server's **Agents** page as *Pending*. Once
   the server administrator approves it, the agent becomes active and can share reports.

To follow progress, open **Central Servers** in the agent and click the server. The **Registration** card
shows the current status.

## What the status means

| Status                        | What it means                                   | What to do                                              |
|:------------------------------|:------------------------------------------------|:--------------------------------------------------------|
| Registering                   | The agent is contacting the server.             | Wait a moment. The page updates by itself.              |
| Registered - pending approval | The server knows the agent and waits for approval. | Ask the server administrator to [approve it](#pending-approval). |
| Active                        | Approved. Reports are shared with the server.   | Nothing.                                                |
| Failed                        | The agent could not register.                   | Find the reason shown on the card in the list below.    |
| Unregistered                  | The agent has not tried to register yet.        | Click **Retry Registration**.                           |

When registration fails for a temporary reason, such as the server being briefly unreachable, the agent
tries again by itself every 15 minutes for about two hours. You can also click **Retry Registration** at
any time.

## Common problems

A failed registration shows a short reason on the **Registration** card. The **What does this mean?** link
next to it opens the matching section below.

### The agent cannot reach the server {#unreachable}

The agent got no answer at the address you entered.

- Check the address for typos. Enter only the base address, for example `https://dqf.example.org`,
  without a slash or anything else at the end.
- Open the address in a browser on the machine where the agent runs. If it does not load there either,
  something in the network is blocking it.
- Ask your IT team whether a firewall or proxy blocks outgoing connections from the agent.

### The server did not respond in time {#timeout}

The agent found the server, but the server took too long to answer.

- The server may be down or busy. Try again later, or ask the server administrator.
- A slow network or proxy can cause this too. Your IT team can raise the time limits, see
  [Configuration](../configuration.md#connecting-to-a-central-server).

### The secure connection failed {#tls}

The agent could not open a secure (HTTPS) connection to the server.

- Check whether the address should start with `https://` or `http://`.
- If the server uses a certificate issued by your organisation, the agent has to be set up to trust it.
  Ask your IT team.

### The server refused access {#unauthorized}

Something in front of the server, such as a proxy or a list of allowed addresses, turned the agent away.
Ask the server administrator whether connections from your site are allowed.

### This agent is already registered {#conflict}

The server already has an entry for this agent and will not accept it again. This usually happens when
the agent moved to a new machine or network address, or when an agent that was already approved tries to
register again.

Ask the server administrator to [delete the old entry](#server-administrators). Then click
**Retry Registration** in the agent.

### Another agent already uses this network address {#rate-limited}

The server accepts one agent per network address, and an agent from your address is already registered.
This is often an old installation at your site.

- Ask the server administrator to [delete the old agent](#server-administrators), then click
  **Retry Registration**.
- If there is no other agent at your site, wait a few minutes and retry. The server may have been
  limiting requests for a short time.

### The server rejected the request {#client-error}

The address probably points to a different application, not the central server.

- Check that the address is the central server's address.
- The agent and the server may run versions that do not work together. Ask the server administrator
  which version the server runs.

### The server reported an internal error {#server-error}

Something went wrong on the server's side. The agent tries again by itself. If the problem continues,
tell the server administrator.

### The server sent an unexpected answer {#incomplete-response}

The server answered, but not in the way the agent expected. Check that the address points to the central
server itself and not to another service or a login page in front of it.

### Something else went wrong {#unknown}

The agent could not tell what went wrong. The **Technical detail** part of the error message and the
agent's log have more information. Share both with your IT team, or
[open an issue](https://github.com/BBMRI-cz/data-quality-framework/issues).

### Stuck at "Registered - pending approval" {#pending-approval}

Registration worked. The agent is now waiting for the server administrator to approve it (see below).
Nothing more is needed on the agent's side.

## For server administrators {#server-administrators}

- **Approve an agent:** open **Agents**, click the agent with status *Pending*, and click **Approve**.
  **Decline** rejects it.
- **Remove an agent:** open the agent and click **Delete Agent**. Do this when an agent moved to a new
  address or has to register again. Deleting also removes the agent's reports from the server.
- The server accepts only one agent per network address. Two agents at one site that share an address
  cannot both register.

## Where to find more information

- **In the agent:** the server's page shows the **Registration** card and the **Interaction History**
  list with every registration attempt and its result.
- **On the server:** the **Agents** page lists all agents and their status.
- **Logs:** your IT team can look at the agent and server logs. With Docker, use
  `docker logs <container name>`.
