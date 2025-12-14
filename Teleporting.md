Vampire Teleport Ability – Design & Implementation Plan
1. High-Level Concept

Ability name: Teleport
Type: Active, channeled (hold key)
Activation:

Player activates ability (your existing system)

While active, holding V:

Client raycasts from player view

Shows black preview particles at valid teleport location

On release (or press, depending on design):

Server validates location

Teleports player

Black particles at start + end

Plays vanilla teleport sound

Key goals

✅ Reuse existing ability framework

✅ Client-side preview, server-side execution

✅ Fully multiplayer-safe

✅ No client-authoritative teleporting

2. Core Design Principles (Important for Multiplayer)
   Concern	Solution
   Cheat prevention	Server validates teleport location
   Smooth UX	Client handles raycast + preview
   Sync	Packets for teleport request + particles
   Authority	Server performs teleport
3. Ability Lifecycle (State Flow)
   Idle
   ↓ (activate ability)
   Ability Active
   ↓ (hold V)
   Preview Mode (client-side)
   ↓ (release V)
   Teleport Request → Server Validation → Teleport
   ↓
   Cooldown

4. Key Components
   4.1 Ability Class (Reuse Existing System)

Create a new ability class:

public class TeleportAbility extends Ability {
// cooldown
// max range
// vampire-only checks (if applicable)
}


Responsibilities

Track cooldown

Check if player can teleport

Integrate with your ability activation logic

4.2 Keybinding (Client Only)
KeyBinding TELEPORT_KEY = new KeyBinding(
"key.vampire.teleport",
GLFW.GLFW_KEY_V,
"key.categories.vampire"
);


Handling logic

Only active when:

Player has Teleport ability

Ability is toggled ON

Holding key → preview

Releasing key → send teleport request packet

5. Raycasting & Location Selection (Client)
   5.1 Raycast Logic

Use vanilla ray tracing:

HitResult hit = player.pick(MAX_DISTANCE, partialTicks, false);


Or more explicit:

Level level = player.level();
Vec3 start = player.getEyePosition(partialTicks);
Vec3 look = player.getLookAngle();
Vec3 end = start.add(look.scale(MAX_DISTANCE));

BlockHitResult hit = level.clip(new ClipContext(
start,
end,
ClipContext.Block.COLLIDER,
ClipContext.Fluid.NONE,
player
));

5.2 Valid Teleport Location Rules

Client-side pre-checks (for UX only):

Must hit a block

Target block must have 2-block-tall air space

Not inside liquids

Not outside world bounds

Server will re-check everything.

6. Preview Particles (Client-Side)
   Behavior

While holding V

Spawn black particles at candidate teleport position

Update position every tick

Particle Suggestions

ParticleTypes.SMOKE

ParticleTypes.PORTAL

Custom black particle later

level.addParticle(
ParticleTypes.SMOKE,
x + 0.5,
y + 1,
z + 0.5,
0, 0.01, 0
);


⚠ Do NOT spawn particles server-side for preview

7. Teleport Request (Client → Server Packet)
   Packet: TeleportRequestPacket

Data

Target position (BlockPos or Vec3)

Ability ID (optional, for generic handler)

public record TeleportRequestPacket(BlockPos targetPos) {}


Sent only when key is released.

8. Server-Side Teleport Validation
   Server Handler Responsibilities

Verify player:

Has teleport ability

Ability is active

Cooldown expired

Validate location:

Distance ≤ max range

Chunk loaded

Safe position (air blocks)

Not inside solid blocks

Not inside protected zones (if applicable)

Perform teleport:

player.teleportTo(x + 0.5, y, z + 0.5);


Apply cooldown

Trigger effects (particles + sound)

9. Teleport Effects (Server → Clients)
   9.1 Particles (Server-Side)

Spawn particles server-side so all players see them:

((ServerLevel) level).sendParticles(
ParticleTypes.PORTAL,
player.getX(), player.getY() + 1, player.getZ(),
50,
0.5, 1, 0.5,
0.1
);


Do this:

At origin

At destination

9.2 Sound (Vanilla)

Use a vanilla teleport-like sound:

Recommended

SoundEvents.ENDERMAN_TELEPORT

SoundEvents.CHORUS_FRUIT_TELEPORT

level.playSound(
null,
player.blockPosition(),
SoundEvents.ENDERMAN_TELEPORT,
SoundSource.PLAYERS,
1.0F,
1.0F
);

10. Cooldown & Balance Hooks

Expose config values:

Max teleport range

Cooldown duration

Vampire level requirement

Hunger / blood cost (future)

[teleport]
maxRange = 24
cooldownTicks = 200

11. Multiplayer Safety Checklist ✅

✔ Teleport execution only on server
✔ Client only suggests target
✔ Server validates all conditions
✔ Particles & sound synced to all players
✔ No client-side position manipulation


13. Minimal Implementation Order (Recommended)
- Ability class stub
- Keybind detection
- Client raycast + preview particles
- Packet system
- Server validation + teleport
- Server particles + sound
- Cooldown integration