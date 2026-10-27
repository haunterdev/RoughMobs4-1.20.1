# Rough Mobs 4

Harder, smarter vanilla mobs for Minecraft 1.20.1.

Rough Mobs 4 is a 1.20.1 port of [Rough Mobs Revamped](https://github.com/p1ut0nium/RoughMobsRevamped) by p1ut0nium,
which continues Lellson's Rough Mobs and Rough Mobs 2. It brings the features Rough Mobs 1 and 2 had and Revamped
dropped back as config options, and a Classic Preset switches the defaults to Rough Mobs 1 or 2 numbers in one line.

Install it on the server and on every client: the client draws the hostile bats, the boss lightning and the boss
fog. Every feature has a config option, and each mob group can be switched off as a whole.

## Features

- **Equipment.** Zombies and skeletons spawn with weapons, armor and enchantments from weighted lists, per dimension.
  Each list entry is `item;weight[;dimension]`.
- **Attributes.** Extra health, follow range, knockback resistance and speed per mob type, from one list
  (`entity;attribute;operation;value`).
- **Zombies, husks, zombie villagers, drowned.** Leap at their target, cause hunger on hit, ride zombie horses, break
  crops, torches and lamps when they have nothing to chase, and can burn in sunlight with a helmet on.
- **Skeletons and strays.** Switch between bow and melee weapon by distance, ride skeleton horses, faster bow cooldown,
  can burn with a helmet on.
- **Creepers.** Turn invisible close to their target, shorter fuse, bigger blast, can burn in sunlight and explode
  when set on fire.
- **Spiders and cave spiders.** Take reduced fall damage, slow their target and spin a web on it, carry zombie,
  skeleton, witch or cave spider riders.
- **Endermen.** Drop an extra ender pearl, teleport attackers away, blind their target, steal the held or worn item.
  An immunity item in the inventory protects against teleporting and stealing.
- **Endermites.** Teleport attackers away and wither nearby non-monster mobs.
- **Witches.** Buff nearby monsters with strength, resistance and speed, throw lingering potions, release bats on death.
- **Slimes and magma cubes.** Split off babies when hit, hit with extra knockback; magma cubes regenerate in lava and
  small ones leave lava behind.
- **Silverfish.** Split in two when attacked.
- **Zombified piglins.** Get angry when touched, when blocks break near them, or always within a set range.
- **Blazes.** Push attackers back, set fire to whatever touches them, explode on death.
- **Ghasts.** Immune to projectiles, drop primed TNT on their target, bigger fireball blasts.
- **Guardians.** Leave water behind on death.
- **Withers.** Push attackers back and summon wither skeletons on a timer.
- **Undead horses.** Burn in sunlight without a rider, pick up a nearby zombie or skeleton as a rider.
- **Spawns and targets.** Extra Nether spawns for wither skeletons, blazes and magma cubes. Two optional lists, both
  off by default: mobs that always attack a given target (`targetAttacker`, zombies after pigs for example), and
  mobs that can't target a given mob after a stray hit (`targetBlocker`, skeletons that shoot each other).
- **Where rough mobs appear.** A minimum distance from world spawn, a minimum level for the nearest player, and an
  underground-only switch.
- **Game Stages and Serene Seasons.** Equipment and abilities can wait for a game stage, and rough mobs can be limited
  to chosen seasons.

### Classic options (Rough Mobs 1 and 2)

- **Slime on head** (`slimeOnHead`). A small slime or magma cube jumps onto a player's head, slowing them and
  causing nausea. Off by default.
- **Bat vampire** (`batVampire`). Bats latch onto a player and drink their blood. Off by default.
- **Snowballs melt in the Nether.**
- **Fire-immune mobs** and **reduced spawn rates** per mob and dimension, from lists.
- **Boss babies** and **boss explosion**, with bosses on. A boss zombie spawns baby zombies when hit, and a boss
  skeleton's hit sets off an explosion at the player.
- **Creeper particles** (`creeperInvisibleParticles`), so an invisible creeper can still be spotted.
- **Undead horses burning with a rider, witches buffing every mob, enderman nausea, endermite wither range, blaze
  burn time, a random number of witch bats** (`witchBatsOnDeathRandom`).

A slime or bat on a player's head stays there over a logout.

The `Classic` section's `_ClassicPreset` takes `rmr` (default), `rm2` or `rm1`. On the next start the preset's
values are written into the config once, and the server log lists each one. A value changed by hand after that
keeps its value; switching presets again moves only the options still at the old preset's value.
`_ClassicPresetApplied` records the preset last written. `rm1` also turns on slime on head, bat vampire and creeper
particles, and its witches drop 3 to 7 bats.

### Bosses (off by default)

Set `_BossGlobal_BossesEnabled = true` to turn them on; `zombie_BossChance` (zombies and husks) and
`skeleton_BossChance` set how often (1 in X). Boss zombies, boss husks and boss skeletons spawn in diamond gear with
a name and title, call down lightning, summon bat swarms, and fill the air around them with a poisonous green fog
that damages players inside it. The fog colour and distance are the server's settings. Switching
bosses off again takes the fog, swarms, babies and explosion from bosses already in a world; they stay as named
mobs in boss gear.

## Requirements

- Minecraft 1.20.1
- Forge 47.1.0 or newer

Game Stages and Serene Seasons are optional.

## Configuration

`config/roughmobs4.toml`, written on first start and read when the game starts. The options keep Rough Mobs
Revamped's names, defaults and sections. List values copied from a Rough Mobs Revamped 1.12.2 config work as they
are: 1.12.2 entity, block and attribute names (`zombie_pigman`, `generic.maxHealth`) and numeric dimension ids
(`-1`, `0`, `1`) are translated.

To carry over a whole old config, put a Rough Mobs 2 `roughmobs.cfg` or a Rough Mobs Revamped 1.12.2
`roughmobsrevamped.cfg` in `config/` and set `_ClassicImportLegacyConfig` to its file name. The next start copies
every value that has a Rough Mobs 4 option into `roughmobs4.toml`, clears the key, and lists what it did in
`config/roughmobs4-import.log`.

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

## Credits

- Lellson: Rough Mobs and Rough Mobs 2
- p1ut0nium: Rough Mobs Revamped
- xy: Rough Mobs 4

## License

GPL-3.0, the same as Rough Mobs Revamped. See [LICENSE](LICENSE).

## Changes from the original

- Bosses are off by default.
- The Classic options and the Classic Preset above are new; Revamped 1.12.2 does not have them.
- Boss lightning can be turned off (`BossGlobal_LightningBolt`), and boss names can carry a title
  (`BossGlobal_Honorifics`), from Revamped's 1.16.5 branch. The lightning strikes once, when the boss spawns.
- Husks can become bosses. Revamped's 1.16.5 branch registered a husk champion and never spawned it.
- The boss fog uses the server's settings.
- Old Rough Mobs 2 and Revamped configs can be imported (`_ClassicImportLegacyConfig`).
- Witches can be told to skip mobs (`BuffBlacklist`) or to buff every mob (`BuffAllMobs`), and drop a random
  number of bats (`witchBatsOnDeathRandom`).
- The blaze burn time is an option (`blazeFlameTouchSeconds`, 8 as in Revamped).
- Several Revamped bugs are fixed, among them: a boss with more than one bat out crashing on death, one bat swarm
  and fog timer shared by every boss, boss settings read before the config loaded, the skeleton boss dropping rotten flesh, and the target blocker printing to the console.
- `BabyBurn` is kept so old configs load; 1.20.1 baby zombies burn in sunlight whatever it is set to.
