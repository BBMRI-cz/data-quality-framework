# Connecting an Agent to a Central Server

1. In the agent, open **Central Servers**, click **Add Central Server**, enter a name and the server's
   address, and click **Register Server**.
2. The server administrator approves the agent.

The **Registration** card on the server's page shows the progress. If registration fails, fix the cause
below and click **Retry Registration**.

## Problems

### Cannot reach the server {#unreachable}

Check the address. Use only the base address, such as `https://dqf.example.org`.

### The server did not respond in time {#timeout}

The server may be down or busy. Try again later.

### Secure connection failed {#tls}

Check whether the address starts with `https://` or `http://`.

### Access refused {#unauthorized}

Ask the server administrator to allow connections from your site.

### Already registered {#conflict}

Ask the server administrator to [delete the old agent](#server-administrators), then retry.

### Another agent uses this address {#rate-limited}

Only one agent per network address is allowed. Ask the server administrator to
[delete the old agent](#server-administrators), then retry.

### Request rejected {#client-error}

The address probably does not point to the central server. Check it.

### Server error {#server-error}

The agent retries by itself. If it keeps failing, tell the server administrator.

### Unexpected answer {#incomplete-response}

The address probably does not point to the central server. Check it.

### Unknown error {#unknown}

Send the error message from the **Registration** card to your IT team.

### Waiting for approval {#pending-approval}

Registration worked. The server administrator now has to approve the agent.

## For server administrators {#server-administrators}

Open **Agents** and click the agent. Click **Approve** to accept it, or **Delete Agent** to remove it.
