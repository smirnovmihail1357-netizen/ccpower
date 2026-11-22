# CC: Power

**RF energy consumption for [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) computers and turtles.**

Minecraft 1.21.1 · NeoForge 21.1.x · requires CC: Tweaked 1.116+

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

The Jade energy bar from 0.8.0 is not part of this Fabric port (there is no
Jade integration here). The admin-only `/ccpower debug` command is available,
but has no visible effect without a tooltip integration.

## Lua API

Full reference with all methods and examples: [docs/API.md](docs/API.md).

Every computer has a `ccpower` API:

| Method | Returns |
|---|---|
| `ccpower.getEnergy()` | Current RF in the internal buffer/battery |
| `ccpower.getCapacity()` | Capacity of that store |
| `ccpower.getChargeRate()` | RF received on the last server tick |
| `ccpower.getDischargeRate()` | RF spent on the last server tick |

Rates are measured per game tick (RF/t). A computer that is turned off still
reports the charge it receives.

The Recharge Dock is also a peripheral of type `ccpower_dock`. An adjacent
computer (or a wired modem) can read its internal state:

```lua
local dock = peripheral.find("ccpower_dock")
dock.getEnergy()       -- RF in the dock buffer
dock.getCapacity()     -- buffer capacity
dock.getPullRate()     -- RF/t pulled from neighbours
dock.getChargeRate()   -- RF/t delivered to the turtle
```

Computers and turtles also expose `getEnergy`, `getCapacity`,
`getChargeRate` and `getDischargeRate` on their standard `computer` / `turtle`
peripherals, so another computer can inspect them.

## Building (Fabric, MC 1.20.4)

```
cd fabric && ./gradlew build
```

Java 17 or newer. The jar lands in `fabric/build/libs/`.

## License

[MIT](LICENSE). Not an official CC: Tweaked addon; CC: Tweaked is a separate
project by its own authors.
