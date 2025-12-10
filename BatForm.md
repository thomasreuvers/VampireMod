# Bat Transformation Implementation Plan - AI Agent Instructions
## Forge 1.20.1 - Complete Model Replacement System

---

## AGENT OBJECTIVE
Implement a complete bat transformation system where players physically become bats, replacing the player model entirely. This includes model rendering, hitbox changes, sound replacement, and proper multiplayer synchronization.

---

## PROJECT CONTEXT

### Existing Systems (DO NOT CREATE THESE)
- ✅ Capability system for player data storage
- ✅ Capability provider and attacher (already implemented)
- ✅ Ability system for triggering transformations
- ✅ Network packet infrastructure for syncing

### What Agent Must Create
- Client-side bat model renderer with proper rotation
- First-person view handling (hide player arms)
- Hitbox/dimension changes for bat size
- Sound replacement system
- Ambient bat sound system
- Integration with existing capability/ability systems

### Current Issues to Fix
1. ❌ Bat model rotation doesn't follow player movement
2. ❌ Player arms visible in first-person view
3. ❌ Player hitbox unchanged (still full player size)
4. ❌ Player sounds still audible (footsteps, hurt sounds)
5. ❌ No ambient bat sounds
6. ❌ Camera height wrong (still at player eye level)

---

## IMPLEMENTATION PLAN

---

## PHASE 1: Client-Side Rendering System

### FILE 1: `BatPlayerRenderer.java`
**Location**: `src/main/java/com/yourmod/client/renderer/BatPlayerRenderer.java`

**Purpose**: Handle all visual rendering of bat model, replacing player model in all view modes.

#### Requirements:
1. **Class Structure**:
    - Annotate with `@Mod.EventBusSubscriber(modid = YourMod.MODID, value = Dist.CLIENT)`
    - Static fields for bat model and texture
    - All methods must be static

2. **Bat Model Initialization**:
    - Create static `BatModel<Player>` field
    - Initialize lazily on first render
    - Use `ModelLayers.BAT` for model layer
    - Texture: `minecraft:textures/entity/bat.png`

3. **Event Handler: Third-Person Rendering**:
    - Subscribe to `RenderPlayerEvent.Pre`
    - Check if player should render as bat (via capability)
    - If yes: `event.setCanceled(true)` and call custom render method
    - If no: Allow normal player rendering

4. **Custom Bat Render Method**:
   ```
   Inputs needed:
   - Player entity
   - PoseStack from event
   - MultiBufferSource from event
   - Packed light from event
   - Partial tick from event
   
   Process:
   1. Push pose stack
   2. Translate to correct Y position (1.3D for player height)
   3. Calculate body rotation with interpolation:
      - Use player.yBodyRotO and player.yBodyRot
      - Interpolate with partial tick: Mth.rotLerp(partialTick, yBodyRotO, yBodyRot)
      - Apply rotation: Axis.YP.rotationDegrees(180.0F - interpolatedYaw)
   4. Scale bat model (2.0F in all axes - bat is very small)
   5. Handle crouching (translate Y by -0.15D / scale if crouching)
   6. Setup animations:
      - limbSwing: player.walkAnimation.position(partialTick)
      - limbSwingAmount: player.walkAnimation.speed(partialTick)
      - ageInTicks: player.tickCount + partialTick
      - netHeadYaw: player.getYHeadRot() - player.yBodyRot (relative)
      - headPitch: player.getXRot()
   7. Get vertex consumer with bat texture
   8. Call batModel.renderToBuffer()
   9. Pop pose stack
   ```

5. **Event Handler: First-Person Hands**:
    - Subscribe to `RenderHandEvent`
    - Check if local player should render as bat
    - If yes: `event.setCanceled(true)` (hides hands/items)

6. **Event Handler: First-Person Arms**:
    - Subscribe to `RenderArmEvent` (if exists in 1.20.1)
    - Check if local player should render as bat
    - If yes: `event.setCanceled(true)` (hides arms)

7. **Helper Method: shouldRenderAsBat()**:
   ```
   Input: Player entity
   Output: boolean
   
   Process:
   - Get player's capability (use YOUR existing capability)
   - Call isBatForm() method on capability
   - Return result (default false if capability missing)
   
   Note: Must handle both server and client-side players
   ```

#### Critical Details:
- **Rotation Bug Fix**: Use `yBodyRot` NOT `getYRot()` - this is critical
- **Interpolation**: Always use `Mth.rotLerp()` for smooth rotation
- **Relative Head Yaw**: Subtract body yaw from head yaw before passing to animation
- **Scale First**: Apply scaling before any other transformations
- **Lazy Model Init**: Only create model once, reuse for all renders

#### Expected Behavior:
- Third-person: Bat model visible, rotates smoothly with player
- First-person: No player parts visible (immersive bat view)
- Swimming: Bat animates correctly
- Sneaking: Bat lowers to ground
- All players: Other players see your bat form in multiplayer

---

## PHASE 2: Hitbox and Dimension System

### FILE 2: `BatTransformationHandler.java`
**Location**: `src/main/java/com/yourmod/common/BatTransformationHandler.java`

**Purpose**: Manage entity physics, dimensions, and server-side logic.

#### Requirements:

1. **Class Structure**:
    - Annotate with `@Mod.EventBusSubscriber(modid = YourMod.MODID)`
    - Static methods only (it's an event subscriber)
    - No client-side only code (this runs on both sides)

2. **Bat Dimensions Constant**:
   ```
   BAT_DIMENSIONS = EntityDimensions.scalable(0.5F, 0.9F)
   - Width: 0.5 blocks (vs 0.6 for player)
   - Height: 0.9 blocks (vs 1.8 for player)
   - Scalable: true (allows pose changes)
   ```

3. **Event Handler: Entity Size**:
    - Subscribe to `EntityEvent.Size`
    - Check if entity is Player
    - Check if player should be bat (via capability)
    - If yes:
        - `event.setNewSize(BAT_DIMENSIONS)`
        - `event.setNewEyeHeight(BAT_DIMENSIONS.height * 0.85F)`
    - This changes:
        - Collision box (can fit through smaller spaces)
        - Camera height (view from bat's eyes)
        - Reach distance (shorter reach)

4. **Event Handler: Living Tick**:
    - Subscribe to `LivingEvent.LivingTickEvent`
    - Check if entity is ServerPlayer (server side only)
    - Check if player should be bat
    - If yes, apply:
        - `player.maxUpStep = 0.2F` (can't step full blocks)
        - Optional: Slow falling when in air
        - Optional: Night vision effect at night
    - If no (not bat):
        - Restore defaults: `player.maxUpStep = 0.6F`

5. **Helper Method: shouldRenderAsBat()**:
    - Same as renderer version
    - Must work server-side and client-side

#### Critical Details:
- **Server Authority**: Dimension changes MUST happen on server
- **Sync Trigger**: When toggling bat form, call `player.refreshDimensions()`
- **Both Sides**: This handler runs on both client and server
- **Tick Performance**: Only modify attributes when state changes, not every tick

#### Expected Behavior:
- Player physically smaller (can fit through 1-block tall spaces)
- Camera lowered to bat eye height (~0.75 blocks)
- Cannot step up full blocks (must jump)
- Collision matches visual size

---

## PHASE 3: Sound Replacement System

### FILE 3: `BatSoundHandler.java`
**Location**: `src/main/java/com/yourmod/common/BatSoundHandler.java`

**Purpose**: Replace player sounds with bat sounds dynamically.

#### Requirements:

1. **Class Structure**:
    - Annotate with `@Mod.EventBusSubscriber(modid = YourMod.MODID)`
    - Static methods only
    - Client and server compatible

2. **Sound Mapping Method**:
   ```
   Input: SoundEvent (the original player sound)
   Output: SoundEvent (bat replacement) or null
   
   Mappings:
   - "step" sounds → SoundEvents.BAT_TAKEOFF
   - "hurt" sounds → SoundEvents.BAT_HURT
   - "death" sounds → SoundEvents.BAT_DEATH
   - "ambient"/"idle" → SoundEvents.BAT_AMBIENT
   - Everything else → null (no replacement)
   
   Implementation:
   - Get ResourceLocation from SoundEvent
   - Check path string for keywords
   - Return appropriate bat sound
   ```

3. **Event Handler: Play Sound**:
    - Subscribe to `PlaySoundEvent` (if available in Forge 1.20.1)
    - Get sound position from event
    - Find nearby players (within 2 blocks of sound source)
    - For each nearby player:
        - Check if they should be bat
        - If yes:
            - Get bat sound replacement
            - If replacement exists:
                - Cancel original sound: `event.setSound(null)`
                - Play bat sound at same position/volume/pitch

4. **Alternative: Living Sound Events**:
    - If `PlaySoundEvent` doesn't work reliably
    - Subscribe to player-specific sound events
    - Override at source rather than intercept

#### Critical Details:
- **Sound Position**: Must match player position exactly
- **Volume/Pitch**: Preserve from original sound
- **Source**: Use same SoundSource (player, block, etc.)
- **Distance Check**: Only replace sounds near transformed players

#### Expected Behavior:
- Walking: Bat wing flap sounds instead of footsteps
- Taking damage: Bat squeak instead of "oof"
- Death: Bat death sound
- No more player voice sounds while bat

---

## PHASE 4: Ambient Sound System

### FILE 4: Add to `BatTransformationHandler.java`
**Purpose**: Play periodic bat sounds while transformed.

#### Requirements:

1. **Event Handler: Player Tick**:
    - Subscribe to `TickEvent.PlayerTickEvent`
    - Only run on server side: `event.side == LogicalSide.SERVER`
    - Only run on END phase: `event.phase == TickEvent.Phase.END`
    - Check if player should be bat
    - If yes, apply ambient sound logic

2. **Ambient Idle Sounds**:
   ```
   Frequency: Every 80 ticks (4 seconds)
   Chance: 30% when check occurs
   Sound: SoundEvents.BAT_AMBIENT
   Volume: 0.5F
   Pitch: 1.0F
   
   Logic:
   if (player.tickCount % 80 == 0) {
       if (random.nextFloat() < 0.3F) {
           play sound at player position
       }
   }
   ```

3. **Flying Sounds**:
   ```
   Trigger: Player in air AND moving upward
   Frequency: Every 10 ticks (0.5 seconds)
   Sound: SoundEvents.BAT_LOOP or BAT_TAKEOFF
   Volume: 0.3F
   Pitch: 1.0F
   
   Conditions:
   - !player.onGround()
   - player.getDeltaMovement().y > 0
   ```

4. **Movement Sounds**:
   ```
   Trigger: Player moving horizontally
   Frequency: Based on movement speed
   Sound: SoundEvents.BAT_TAKEOFF
   Volume: Based on speed
   ```

#### Critical Details:
- **Server Side Only**: Sounds must originate from server
- **Performance**: Use modulo checks, not every tick
- **Randomization**: Add variety, don't make robotic
- **Context Aware**: Different sounds for flying vs idle

#### Expected Behavior:
- Occasional bat squeaks while standing still
- Wing flapping sounds while flying/jumping
- More frequent sounds when moving fast
- Sounds heard by all nearby players

---

## PHASE 5: Capability Integration

### FILE 5: Modifications to Existing Capability
**Purpose**: Ensure bat state is properly stored and synced.

#### Requirements:

1. **Capability Interface Additions**:
   ```
   Add to your existing capability interface:
   - boolean isBatForm()
   - void setBatForm(boolean batForm)
   ```

2. **Capability Implementation**:
   ```
   Add field:
   - private boolean batForm = false;
   
   Implement methods:
   - isBatForm() returns this field
   - setBatForm(boolean) sets field + triggers sync
   ```

3. **NBT Serialization**:
   ```
   In serializeNBT():
   - nbt.putBoolean("BatForm", this.batForm);
   
   In deserializeNBT():
   - this.batForm = nbt.getBoolean("BatForm");
   ```

4. **Sync Enhancement**:
   ```
   In setBatForm():
   1. Set the field value
   2. If on server side:
      - Call player.refreshDimensions() (critical!)
      - Sync to player's client (your existing sync)
      - Sync to all tracking players (your existing sync)
   ```

#### Critical Details:
- **refreshDimensions()**: MUST be called when toggling bat form
- **Sync Everywhere**: State must reach all clients who can see the player
- **Persistence**: Bat form must survive logout/login
- **Initial Sync**: New clients must receive state when player enters render distance

#### Expected Behavior:
- Player logs out as bat, logs in still as bat
- Other players see bat form immediately when entering render distance
- State persists through death (or resets, based on design choice)
- Server restarts don't lose transformation state

---

## PHASE 6: Ability Integration

### FILE 6: `BatTransformAbility.java`
**Location**: `src/main/java/com/yourmod/ability/BatTransformAbility.java`

**Purpose**: Allow players to toggle bat form via existing ability system.

#### Requirements:

1. **Class Structure**:
    - Extend your existing ability base class
    - Follow your project's ability pattern
    - Implement required abstract methods

2. **Ability Properties**:
   ```
   - Name: "Bat Transformation"
   - Type: Toggle (on/off, not instant)
   - Cooldown: 0-5 seconds (design choice)
   - Activation: Keybind or command
   ```

3. **Activation Logic**:
   ```
   On activate (server side):
   1. Get player's capability
   2. Get current bat form state
   3. Toggle: setBatForm(!currentState)
   4. Capability's setBatForm will handle sync
   
   Optional enhancements:
   - Play transformation particle effects
   - Play transformation sound
   - Apply brief invulnerability during transform
   - Prevent activation while in combat
   ```

4. **Deactivation Logic**:
   ```
   Same as activation (it's a toggle)
   OR
   Separate deactivation method if your system requires it
   ```

5. **Validation**:
   ```
   Before allowing activation:
   - Check if player has permission
   - Check cooldown
   - Check if in valid location (not in solid block)
   - Check if transform would cause suffocation
   ```

#### Critical Details:
- **Server Authority**: Ability activation must go through server
- **Validation**: Prevent exploits (transforming to escape death, etc.)
- **Feedback**: Give player clear indication of success/failure
- **Integration**: Use existing ability registration system

#### Expected Behavior:
- Player presses keybind → transforms to bat
- Press again → transforms back to human
- Cooldown prevents spam
- Works in multiplayer with proper sync

---

## PHASE 7: Testing and Validation

### Testing Checklist (Agent Must Verify)

#### Single Player Tests:
1. **Rendering**:
    - [ ] Transform to bat → model appears
    - [ ] Bat rotates when player turns
    - [ ] Walk forward → bat faces movement direction
    - [ ] Strafe → bat rotates correctly
    - [ ] First person → no arms/items visible
    - [ ] Third person (F5) → bat visible and animated
    - [ ] Crouch → bat lowers

2. **Physics**:
    - [ ] Stand next to 1-block high space → can fit through
    - [ ] Stand next to 1-block tall tunnel → can walk through
    - [ ] Try to step up full block → cannot (must jump)
    - [ ] Camera lowered compared to normal player height
    - [ ] Jump height unchanged (or adjusted as intended)

3. **Sounds**:
    - [ ] Walk → hear bat wing flaps, not footsteps
    - [ ] Take damage → hear bat squeak
    - [ ] While idle → occasional ambient bat sounds
    - [ ] While flying → wing flapping sounds
    - [ ] Transform back → normal player sounds return

4. **Persistence**:
    - [ ] Transform → save/quit → load → still bat
    - [ ] Transform → die → respawn → check if still bat (design choice)
    - [ ] Transform → change dimension → still bat

#### Multiplayer Tests (Server + 2+ Clients):
1. **Visual Sync**:
    - [ ] Player A transforms → Player B sees bat model
    - [ ] Player B joins while Player A is bat → Player B sees bat
    - [ ] Player A transforms → Player C (far away) approaches → sees bat
    - [ ] Multiple players transform simultaneously → all render correctly

2. **Physics Sync**:
    - [ ] Player A transforms → Player B sees smaller hitbox
    - [ ] Player A fits through small space → Player B sees this correctly
    - [ ] Both players transform → both have bat physics

3. **Sound Sync**:
    - [ ] Player A transforms and walks → Player B hears bat sounds
    - [ ] Player A takes damage → Player B hears bat hurt sound
    - [ ] Player A idles → Player B hears ambient bat sounds

4. **Latency Handling**:
    - [ ] Simulate lag → transformations still sync
    - [ ] Player joins with poor connection → still sees bat forms
    - [ ] Rapid toggling → no desyncs or visual glitches

#### Edge Case Tests:
1. [ ] Transform while in water → behaves correctly
2. [ ] Transform while riding entity → dismounts or prevents transform
3. [ ] Transform while in boat → exits or prevents transform
4. [ ] Transform in solid block → doesn't suffocate or prevents transform
5. [ ] Transform at world height limit → works correctly
6. [ ] Transform while on fire → fire visual matches smaller size
7. [ ] Transform while poisoned → particles match size
8. [ ] Transform with armor equipped → armor invisible (design choice)
9. [ ] Transform while holding item → item invisible or drops

---

## CRITICAL IMPLEMENTATION NOTES

### For AI Agent:

1. **Use Existing Systems**:
    - DO NOT create new capability infrastructure
    - DO NOT create new packet system
    - DO use the existing capability interface (add methods)
    - DO use the existing ability system (create new ability)

2. **Project Structure Detection**:
    - Find existing capability interface/class
    - Find existing ability base class
    - Find existing network handler
    - Adapt code to match existing patterns

3. **Naming Conventions**:
    - Match the project's naming style
    - Use project's package structure
    - Follow existing code formatting

4. **Import Statements**:
    - Use project's mod ID
    - Reference existing classes correctly
    - Check all imports are valid for Forge 1.20.1

5. **Version Compatibility**:
    - All code must work in Forge 1.20.1
    - Check event names match this version
    - Verify method signatures match this version
    - Some events may have different names in 1.20.1 vs newer versions

### Common Pitfalls:

1. **Rotation Issues**:
    - MUST use `yBodyRot` not `getYRot()`
    - MUST interpolate with `Mth.rotLerp()`
    - MUST subtract body rotation from head rotation

2. **First Person**:
    - MUST cancel both hand and arm render events
    - Check event names for 1.20.1 (may differ)

3. **Hitbox**:
    - MUST call `refreshDimensions()` when toggling
    - MUST set both size and eye height
    - Eye height affects camera position

4. **Sounds**:
    - MUST play on server side, not client
    - MUST use correct SoundSource
    - Check if `PlaySoundEvent` exists in 1.20.1

5. **Sync**:
    - MUST sync to player's own client
    - MUST sync to all tracking players
    - MUST sync on login
    - MUST sync when entering render distance

### Code Quality Requirements:

1. **Null Safety**:
    - Always check player != null
    - Always check capability.isPresent()
    - Handle Optional properly

2. **Side Checking**:
    - Client-only code: `@OnlyIn(Dist.CLIENT)` or check side
    - Server logic: Check if ServerPlayer
    - Sounds: Play on server, hear on client

3. **Performance**:
    - Static model instance (don't create every frame)
    - Lazy initialization
    - Modulo checks for periodic actions
    - Don't do heavy operations every tick

4. **Error Handling**:
    - Catch exceptions in event handlers
    - Log errors with context
    - Fail gracefully (don't crash game)

---

## SUCCESS CRITERIA

The implementation is complete when:

1. ✅ Bat model renders in all view modes
2. ✅ Bat rotates correctly with player movement
3. ✅ First-person view shows no player parts
4. ✅ Player hitbox matches bat size
5. ✅ Camera at correct height for bat
6. ✅ All sounds replaced with bat sounds
7. ✅ Ambient bat sounds play periodically
8. ✅ State persists across logout/login
9. ✅ Multiplayer sync works perfectly
10. ✅ No console errors or warnings
11. ✅ All tests in checklist pass

---

## DELIVERABLES

Agent must provide:

1. **Complete Source Files**:
    - BatPlayerRenderer.java (client-side)
    - BatTransformationHandler.java (common)
    - BatSoundHandler.java (common)
    - BatTransformAbility.java (ability)
    - Capability modifications (documented changes)

2. **Integration Instructions**:
    - Where to place each file
    - What to modify in existing files
    - Registration code needed
    - Configuration values to adjust
---

## TIMELINE ESTIMATE

- Phase 1 (Rendering): 2-3 hours
- Phase 2 (Hitbox): 1-2 hours
- Phase 3 (Sounds): 2-3 hours
- Phase 4 (Ambient): 1 hour
- Phase 5 (Capability): 1 hour
- Phase 6 (Ability): 1-2 hours
- Phase 7 (Testing): 2-3 hours

**Total**: 10-16 hours

---

## SUPPORT RESOURCES

- Forge Documentation: https://docs.minecraftforge.net/
- Minecraft Code Reference: Decompiled sources in IDE
- Existing Project Code: Reference for patterns and conventions
- Event List: Check Forge EventBus for available events