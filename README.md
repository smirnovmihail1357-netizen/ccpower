# CC: Power

**RF energy consumption for [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) computers and turtles.**

Minecraft 1.18.2 · Forge 40.2.x · requires CC: Tweaked 1.101.3+

[English](README.md) · [Русский](README.ru.md)

Computers and turtles from CC: Tweaked become real energy consumers. They draw
RF/FE (the Forge energy system used by Thermal, Mekanism, Create: Power Grid
and many other tech mods) and stop working when the energy runs out.

## How it works

### Computers

- A computer has a **small internal RF buffer** (no crafting, no batteries -
  the buffer is part of the block) that is refilled from **adjacent blocks**
  holding FE energy (energy cells, cables, machines - anything exposing the
  NeoForge energy capability).
- Every game tick a running computer pays: `idle + (program running ? active : 0)
  + every attached peripheral's per-type price + call surcharges`.
- If the buffer cannot cover the tick's cost, the computer **shuts down**
  (a normal CC shutdown - the running program is lost). Turning it back on is
  manual, and the power-on is **gated**: the computer refuses to boot until
  its buffer holds at least `idle + active` RF.
- A powered-off computer still refills its buffer but consumes nothing.

### Turtles

- A turtle is a computer that moves, so it runs on the **same code path** as
  stationary computers: same per-tick bill, same peripherals, same calls.
- It carries a **small internal RF battery** and needs **two kinds of fuel**:
  * vanilla turtle fuel (unchanged) to **move and act**,
  * RF energy to **run its code**.
- Out of RF - the turtle freezes in place. Out of turtle fuel - same as
  vanilla. Turtle **actions** (move, dig, place, ...) also cost a small amount
  of RF per action.
- Charge turtles at the **Drone Dock**.

### Drone Dock

`ccpower:dock` - a block that pulls FE from its six neighbours into an
internal buffer and charges the turtle standing on top of it.
Recipe: iron, redstone block, lightning rod.

## Per-peripheral-type pricing

`config/ccpower-peripherals.toml` is **generated and maintained by the mod**:

- at server start every installed mod is **pre-scanned** and all peripheral
  types it can provide are written to the file, grouped by mod;
- two independent prices per type:
  `[rf_per_tick]` - paid every tick while the peripheral is attached,
  `[rf_per_call]` - paid for each method call;
- unknown types seen later in the world are appended automatically;
- the file is **re-read while the server runs** - edits apply within ~2 seconds
  without a restart.

Defaults are type-aware: an item handler (chest, hopper, furnace, shulker
box) starts at 15 RF/call, a monitor at 4, a modem at 2 (base
`power.peripheralCallRF` = 5). Every type always has both prices; set a price
to `0.0` to disable it.

## Dynamic configuration

`config/ccpower-common.toml` (NeoForge config, hot-reloaded):

| Key | Default | Meaning |
|---|---|---|
| `power.idleRFPerTick` | 2 | Per-tick cost of a running computer with no program |
| `power.activeRFPerTick` | 10 | Extra per-tick cost while a program is running |
| `power.computerBufferRF` | 2000 | Stationary computer's internal buffer capacity |
| `power.computerPullRFPerTick` | 1000 | RF/tick pulled from adjacent blocks |
| `power.peripheralRFPerTick` | 1 | Default tick price for freshly discovered types |
| `power.peripheralCallRF` | 5 | Base call price (curated multipliers per type) |
| `power.surchargeCapPerTick` | 1000 | Max call surcharges carried into one tick |
| `turtle.actionRF` | 4 | RF per turtle action (move, dig, ...) |
| `turtle.bufferRF` | 20000 | Turtle battery capacity |
| `dock.bufferRF` | 50000 | Drone Dock internal buffer |
| `dock.pullRFPerTick` | 1000 | Drone Dock pull rate from neighbours |
| `dock.chargeRFPerTick` | 250 | Drone Dock charge rate |
| `logging.*` | — | What the mod writes to the log |

## Jade

This Forge 1.18.2 build has **no Jade integration**: the 0.8.0 Jade energy bar
uses the modern Jade API and could not be ported. The `/ccpower debug` command
and the `tooltip.ccpower.debug` translation key are present, but no tooltip is
rendered by this build.

## Lua API

Full reference with all methods and examples: [docs/API.md](docs/API.md).

Every computer has a `ccpower` API (`getEnergy()`, `getCapacity()`,
`getChargeRate()`, `getDischargeRate()`). The same methods are added to every
`computer`/`turtle` peripheral so a neighbour can be inspected with
`peripheral.wrap(...)`. The Recharge Dock is exposed as a `ccpower_dock`
peripheral with `getEnergy()`, `getCapacity()`, `getPullRate()` and
`getChargeRate()`.

Admins can toggle the (currently unrendered) diagnostic line with
`/ccpower debug`.

## Building

```
./gradlew build
```

Java 21. The jar lands in `build/libs/`.

## License

[MIT](LICENSE). Not an official CC: Tweaked addon; CC: Tweaked is a separate
project by its own authors.
