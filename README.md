# Rough Mobs 4

Harder, smarter vanilla mobs for Minecraft 1.20.1.

Rough Mobs 4 is a 1.20.1 port of [Rough Mobs Revamped](https://github.com/p1ut0nium/RoughMobsRevamped) by p1ut0nium,
which continues Lellson's Rough Mobs and Rough Mobs 2. It also brings back the Rough Mobs 1 and 2 features that
Revamped left out, as config options.

This is a Client and server side mod.

## Features

- Equipment: zombies and skeletons spawn with weapons, armor and enchantments from weighted lists, per dimension.
- Attributes: extra health, follow range, knockback resistance and speed per mob type.
- Zombies, husks, zombie villagers, drowned: leap at their target, cause hunger on hit, ride zombie horses, break
  crops, torches and lamps, and can burn in sunlight with a helmet on.
- Skeletons and strays: switch between bow and melee weapon by distance, ride skeleton horses, shoot faster.
- Creepers: turn invisible close to their target, shorter fuse, bigger blast, explode when set on fire.
- Spiders and cave spiders: take less fall damage, slow their target and web it, carry mob riders.
- Endermen: drop an extra ender pearl, teleport attackers away, blind their target, steal the held or worn item.
- Endermites: teleport attackers away and wither nearby non-monster mobs.
- Witches: buff nearby monsters, throw lingering potions, release bats on death.
- Slimes and magma cubes: split off babies when hit and knock back harder; magma cubes regenerate in lava.
- Silverfish: split in two when attacked.
- Zombified piglins: get angry when touched or when blocks break near them.
- Blazes: push attackers back, set fire to whatever touches them, explode on death.
- Ghasts: immune to projectiles, drop primed TNT on their target, bigger fireball blasts.
- Guardians: leave water behind on death.
- Withers: push attackers back and summon wither skeletons.
- Undead horses: burn in sunlight without a rider and pick up a zombie or skeleton rider.
- Spawns: extra Nether spawns for wither skeletons, blazes and magma cubes, plus a minimum distance from spawn,
  a minimum player level and an underground-only switch for where rough mobs appear.
- Targets: optional lists of mobs that always attack a given mob, or never target one.
- Game Stages and Serene Seasons: equipment and abilities can wait for a game stage, and rough mobs can be limited
  to chosen seasons.

### Classic options (Rough Mobs 1 and 2)

- Slime on head: a small slime or magma cube jumps onto a player's head, slowing them and causing nausea.
- Bat vampire: bats latch onto a player and drink their blood.
- Snowballs melt in the Nether.
- Fire-immune mobs and reduced spawn rates per mob and dimension.
- Boss babies and boss explosion: a boss zombie spawns baby zombies when hit, and a boss skeleton's hit sets off
  an explosion.
- Creeper particles, so an invisible creeper can still be spotted.

`_ClassicPreset` in the `Classic` section sets the defaults to Rough Mobs Revamped (`rmr`, the default), Rough Mobs 2
(`rm2`) or Rough Mobs 1 (`rm1`) values.

### Bosses

Bosses come from Rough Mobs 2 and Rough Mobs Revamped and are on by default. Set `_BossGlobal_BossesEnabled` to
`false` to turn them off and `true` to turn them on. `zombie_BossChance` and `skeleton_BossChance` set how often one
spawns (1 in X).

Boss zombies, husks and skeletons spawn in diamond gear with a name and title, call down lightning, summon bat swarms
and give off a poison fog.

## Requirements

- Minecraft 1.20.1
- Forge 47.1.0 or newer
- Optional: Game Stages, Serene Seasons

## Configuration

Everything is in `config/roughmobs4.toml`, with Rough Mobs Revamped's option names.

To use an old config, put a Rough Mobs 2 `roughmobs.cfg` or a Rough Mobs Revamped 1.12.2 `roughmobsrevamped.cfg` in
`config/` and set `_ClassicImportLegacyConfig` to its file name.

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

- Bosses can be turned off.
- The Classic options and `_ClassicPreset`.
- Old Rough Mobs 2 and Revamped configs can be imported.
- Husks can become bosses.
- Boss lightning can be turned off and boss names can carry a title, from Revamped's 1.16.5 version.
- Witches can skip chosen mobs, buff every mob, or drop a random number of bats.
- The blaze burn time is an option.
- Fixed: a boss with more than one bat out crashing on death, every boss sharing one bat swarm and fog timer, and the
  skeleton boss dropping rotten flesh.
