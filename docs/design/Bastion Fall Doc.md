**Bastionfall** is a Forge mod for TerraFirmaGreg (TFG) that adds progression-based PvE raid mechanics. Players trigger or attract raids as they advance through technological ages. Raids involve escalating enemy waves targeting important infrastructure, requiring players to defend their bases.

The system is centered around:
- Age Advancement Blocks (AABs)
- Claim-based base defense
- Custom AI-driven mobs with different roles
- Time-based and player-triggered raid events

# When do raids occur
In order to progress through ages, players must fight through a wave

Raids can occur in two ways

## Player triggered event
1. A player places a functional block
2. The player triggers some action with the block
	1. This action is necessary for the player to begin crafting required components in the next age
	2. An easier way to do this might be tying the action to an achievement that triggers a raid. This achievement would then somehow be a necessity for the next age
3. The block begins to execute the action
	1. A raid is triggered when the action begins
	2. A raid is triggered when the action ends

Multiple Age Advancement Blocks (AABs) can exist. These can be organized in tiers. Higher tiers generate more difficult raids

Block actions can be triggered as many times as desired by the player

Players can also easily trigger raids themselves

## Interval based event
Raids occur every `n` Minecraft days. Interval raids use the last used AAB as the faction context (to scale composition). Players can opt in or out of raids.

# Raid Structure

## Raid length
1. Lasts as long as a AAB takes to complete its craft
2. Set number of mobs spawn
	1. A timer still exists counting down the raid. When the timer expires, checks start occurring to check for stale states and end the raid

## Raid Composition
AABs have specific set of NPCs that can spawn. Reoccurring raids spawn based off the previous AAB's entity list. A raid will contain a weighted selection of mobs defined in [[#Global Roles]] and each age's `aged specific roles`.

### Global Roles
1. Melee
2. Ranged
3. Specialist
4. Support
5. Boss

### Age Zero
sticks and stones

##### Age Specific Roles

### Age One

### Age Two

### Age Three

### Age Four

## Optional Objectives

Raid boss runs away at the end of AAB cycle, taunting the player in global chat until next AAB

# Raid Goals
The goal of any raid is to destroy any AAB present in a base's claimed area.

Mobs will pathfind toward AABs and attempt to destroy blocks using:
- Standard block breaking (delayed via timer per block)
- Explosives or area effects (for specialized units)

Only blocks with the tag `bastionfall:raid_breakable` can be targeted.

Destroyed AABs will generate specific negative actions (exploding, fires, etc)

Raids can contain an additional list of targets that raid mobs have a weighted chance of targeting

# Claims

A claim system will be used to create the right context for raids to occur. In order to trigger an AAB's action, it must be placed within a claimed chunk. If an AAB is not within a claimed chunk, its action cannot be triggered. 

To claim chunks, players must craft and place a specific claim block. The claim block will claim `n` chunks around it.

Claimed areas:
- Prevent raid spawns inside
- Are required to activate AABs
- Can be visualized with particle effects or UI

# Rewards
When a raid is defeated, players will receive rewards 

## Monetary
Defeated raids will spawn a chest containing an item that can be sold at a market

## Trader
Defeated raids will trigger a trader event spawning a trader nearby the base after an `n` amount of days

