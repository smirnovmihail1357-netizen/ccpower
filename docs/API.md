# CC: Power — Lua API and peripherals

CC: Power exposes the internal RF state of computers, turtles and the Recharge
Dock to Lua. All energy values are **RF** (Forge Energy, the unit used by
Thermal, Mekanism, Create: Power Grid and others). Rates are measured over the
**last completed server tick**, so they are in **RF/tick** (multiply by 20 for
RF/second at 20 TPS). If a computer is turned off, its buffer still refills and
`getChargeRate()` keeps reporting the incoming energy; `getDischargeRate()` is
`0`.

There are three ways to read the state, depending on where the Lua code runs.

---

## 1. `ccpower` API — the computer itself

Available on **every** CC: Tweaked computer, turtle and pocket computer as a
global API table. No peripheral wrapping needed.

| Method | Returns | Description |
|---|---|---|
| `ccpower.getEnergy()` | number | RF currently stored in the internal buffer (computers) or battery (turtles). |
| `ccpower.getCapacity()` | number | Capacity of that store, from `power.computerBufferRF` / `turtle.bufferRF`. |
| `ccpower.getChargeRate()` | number | RF received on the last server tick: FE pulled from neighbours, or RF delivered by the Recharge Dock. |
| `ccpower.getDischargeRate()` | number | RF spent on the last server tick: tick cost plus turtle action costs. |

```lua
local energy  = ccpower.getEnergy()
local cap     = ccpower.getCapacity()
local inflow  = ccpower.getChargeRate()
local outflow = ccpower.getDischargeRate()

print(("RF: %d/%d  +%d/t  -%d/t"):format(energy, cap, inflow, outflow))
```

---

## 2. Computer / turtle as a peripheral — read another computer

Every computer and turtle is already a CC: Tweaked peripheral of type
`computer` / `turtle`. CC: Power adds these methods to **all** such peripherals,
so one computer can inspect its neighbours (directly or through a wired modem):

| Method | Returns | Description |
|---|---|---|
| `getEnergy()` | number | RF stored in the remote computer's buffer/battery. |
| `getCapacity()` | number | Capacity of the remote store. |
| `getChargeRate()` | number | RF/t received by the remote computer. |
| `getDischargeRate()` | number | RF/t spent by the remote computer. |

```lua
local remote = peripheral.wrap("back")   -- type "computer" or "turtle"
if remote and remote.getEnergy then
  print("Remote RF:", remote.getEnergy(), "/", remote.getCapacity())
  print("Flow:", remote.getChargeRate(), "in /", remote.getDischargeRate(), "out")
end
```

Via a wired modem, use the peripheral name as usual:

```lua
local modem = peripheral.wrap("left")
local remote = modem.getRemotePeripheral("monitor-side-name")
-- or peripheral.find("computer", ...) / peripheral.getNames()
```

These methods are additive: CC: Tweaked's own methods (`getID`, `isOn`,
`getLabel`, `turnOn`, `shutdown`, `reboot`) keep working on the same wrapper.

---

## 3. Recharge Dock as a peripheral — `ccpower_dock`

The Recharge Dock (`ccpower:dock`) is exposed as a peripheral of type
`ccpower_dock` through the standard CC: Tweaked peripheral capability. Place a
computer next to it, or attach it to a wired modem.

| Method | Returns | Description |
|---|---|---|
| `getEnergy()` | number | RF currently in the dock's internal buffer (`dock.bufferRF`). |
| `getCapacity()` | number | Configured buffer capacity (`dock.bufferRF`). |
| `getPullRate()` | number | RF/t the dock pulled from adjacent FE blocks on the last tick (`dock.pullRFPerTick` is the cap). |
| `getChargeRate()` | number | RF/t the dock transferred into the turtle standing on it (`dock.chargeRFPerTick` is the cap). |

```lua
local dock = peripheral.find("ccpower_dock")
if not dock then error("No Recharge Dock nearby") end

local stored, cap = dock.getEnergy(), dock.getCapacity()
local pulled, given = dock.getPullRate(), dock.getChargeRate()

print(("Dock: %d/%d RF  +%d/t in  -%d/t out"):format(stored, cap, pulled, given))
```

---

## 4. Pocket computers and Smart Glasses

All pocket computers (`pocket_computer*`) and Smart Glasses from
**Advanced Peripherals** are now real energy consumers. Advanced Peripherals is
optional: when it is not installed, nothing Smart-Glasses-specific is loaded
and there is no error. Both devices have their **own internal RF buffer**, which
starts full and is **not refilled** — once it runs out the computer shuts down.
`ccpower.getEnergy()` on such a device reports this buffer.

Portable peripheral costs are configurable separately for each device:

| Key (`power.pocket.*` / `power.smartGlasses.*`) | Default (pocket / glasses) | Meaning |
|---|---|---|
| `mode` | `MULTIPLIER` / `MULTIPLIER` | `MULTIPLIER` or `FIXED` — mutually exclusive; one enum, not two flags. |
| `multiplier` | `1.0` / `0.75` | Factor applied to the normal computer's cost formula in MULTIPLIER mode. |
| `fixedRFPerTick` | `2.0` / `2.0` | Exact RF/t in FIXED mode (peripherals and surcharges are ignored). |
| `bufferRF` | `2000` / `2000` | Capacity of the internal buffer. |
| `peripheralRFPerTick` | `1.0` / `1.0` | Flat tick cost per attached peripheral (when not using computer prices). |
| `peripheralCallRF` | `5.0` / `5.0` | Flat RF per peripheral call (when not using computer prices). |
| `useComputerPeripheralCosts` | `true` / `true` | `true` — use the normal computer's per-type prices from `ccpower-peripherals.toml`; `false` — use the flat values above. |

The multipliers are independent, so pocket computers and Smart Glasses can be
tuned separately. In `FIXED` mode the number is the whole per-tick cost.

---

## 5. Jade tooltip and the admin debug command

With [Jade](https://modrinth.com/mod/jade) installed, pointing at a computer,
turtle or Recharge Dock shows its RF store as the standard Jade energy bar with
the usual numeric `current / capacity FE` label. The bar and the numbers are
drawn by Jade itself — the mod only supplies the values.

Administrators (permission level 2) can add a diagnostic line to the computer
tooltip:

```
/ccpower debug
```

The command toggles the mode **per player** and is not persisted. While enabled,
the tooltip shows charge rate, discharge rate and current/capacity for the
targeted computer. Run it again to turn the line off.

---

## 6. Configuration keys affecting these values

From `config/ccpower-common.toml` (hot-reloaded):

| Key | Default | Meaning |
|---|---|---|
| `power.computerBufferRF` | 2000 | Stationary computer buffer capacity. |
| `power.computerPullRFPerTick` | 1000 | Max RF/t pulled from neighbours by a computer. |
| `turtle.bufferRF` | 20000 | Turtle battery capacity. |
| `dock.bufferRF` | 50000 | Recharge Dock buffer capacity. |
| `dock.pullRFPerTick` | 1000 | Max RF/t pulled from neighbours by the dock. |
| `dock.chargeRFPerTick` | 250 | Max RF/t delivered to a turtle by the dock. |

---

# Русский

## Обзор

Мод предоставляет внутреннее состояние компьютеров, черепах и дроностанции
через Lua. Все значения — **РФ** (Forge Energy), скорости измеряются за
**последний завершённый игровой тик**, то есть в **РФ/тик** (умножьте на 20 для
РФ/сек при 20 TPS). Выключенный компьютер продолжает заряжаться, поэтому
`getChargeRate()` показывает incoming-поток, а `getDischargeRate()` равен `0`.

### 1. API `ccpower` — сам компьютер

Глобальная таблица на каждом компьютере, черепахе и карманном компьютере:

| Метод | Возврат | Описание |
|---|---|---|
| `ccpower.getEnergy()` | число | Текущие РФ в буфере/батарее. |
| `ccpower.getCapacity()` | число | Ёмкость хранилища. |
| `ccpower.getChargeRate()` | число | РФ, полученные за последний тик. |
| `ccpower.getDischargeRate()` | число | РФ, потраченные за последний тик. |

### 2. Компьютер/черепаха как периферия

Каждый компьютер — уже периферия CC:T типов `computer`/`turtle`. Мод добавляет
в неё методы `getEnergy()`, `getCapacity()`, `getChargeRate()`,
`getDischargeRate()`, чтобы другой компьютер мог узнать его состояние через
`peripheral.wrap(...)` или проводной модем. Собственные методы CC:T при этом
сохраняются.

### 3. Дроностанция как периферия `ccpower_dock`

| Метод | Возврат | Описание |
|---|---|---|
| `getEnergy()` | число | РФ в буфере станции. |
| `getCapacity()` | число | Ёмкость буфера. |
| `getPullRate()` | число | РФ/тик, подтянутые от соседей. |
| `getChargeRate()` | число | РФ/тик, отданные черепахе. |

### 4. Карманные компьютеры и Smart Glasses

Все `pocket_computer*` и Smart Glasses из мода **Advanced Peripherals** теперь
потребляют энергию. Advanced Peripherals необязателен: если его нет, мод не
загружает ничего связанного с очками и не выдаёт ошибок.

Оба типа устройств имеют **свой внутренний РФ-буфер**, который заполняется
полным при первом использовании и **не восполняется** — когда он иссякнет,
компьютер выключится. `ccpower.getEnergy()` у такого устройства показывает
этот буфер.

Настройки потребления периферии раздельны для каждого устройства:

| Ключ (`power.pocket.*` / `power.smartGlasses.*`) | Дефолт (карманный / очки) | Значение |
|---|---|---|
| `mode` | `MULTIPLIER` / `MULTIPLIER` | `MULTIPLIER` или `FIXED` — взаимоисключающие режимы (один enum). |
| `multiplier` | `1.0` / `0.75` | Множитель стоимости обычного компьютера в режиме MULTIPLIER. |
| `fixedRFPerTick` | `2.0` / `2.0` | Точное РФ/тик в режиме FIXED (периферия и надбавки игнорируются). |
| `bufferRF` | `2000` / `2000` | Ёмкость внутреннего буфера. |
| `peripheralRFPerTick` | `1.0` / `1.0` | Плоская цена за подключённую периферию за тик. |
| `peripheralCallRF` | `5.0` / `5.0` | Плоская цена за вызов периферии. |
| `useComputerPeripheralCosts` | `true` / `true` | `true` — цены компьютера из `ccpower-peripherals.toml`; `false` — свои плоские значения выше. |

Множители независимы, устройства настраиваются отдельно.

### 5. Jade и команда `/ccpower debug`

При установленном Jade наведение на компьютер, черепаху или дроностанцию
показывает стандартную шкалу энергии с числовой подписью. Полосу и числа
рисует сам Jade. Администратор (уровень прав 2) командой `/ccpower debug`
включает для себя дополнительную строку со скоростями зарядки/разрядки и
текущим буфером. Флаг действует на игрока и не сохраняется между сессиями.
