# AzisabaGift

`/promo` plugin for Velocity & Spigot.

## Features

- `/promo <code>` to redeem a code.
- `/(v)azisabagift [args]` to view, modify, and create the code.

## API

There is no documentation currently, but here are the important things to develop a plugin for this plugin:
- `net.azisaba.gift.registry.Registry`
- `net.azisaba.gift.objects.Handler`
- `net.azisaba.gift.objects.ExpirationStatus`
- `net.azisaba.gift.objects.Selector`
- `net.azisaba.gift.proviers.DataProviders`
- `net.azisaba.gift.proviers.types.*`
- `net.azisaba.gift.JSON`

## Why not use [Exposed](https://github.com/JetBrains/Exposed) API?

Exposed requires kotlin-reflect, and it makes Kotlin not able to use `relocate`.
