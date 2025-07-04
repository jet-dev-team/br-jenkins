# Caddy

Caddy is very particular about its configuration file formatting. It will generate excessive log entries if the config file does not use tabs with a 4-space indentation.

To ensure consistency, a `.editorconfig` file is provided.

## How to generate basic auth password for Caddy

```bash
docker run --rm -it caddy:latest caddy hash-password --plaintext 'secret-password' --algorithm bcrypt
```

## How to Use Staging for SSL Generation

To ensure you don’t hit the SSL generation limit during development, you can use ACME staging. Simply add this to the main block:

```
{
    email hello@jet.dev
    acme_ca https://acme-staging-v02.api.letsencrypt.org/directory
}
```
