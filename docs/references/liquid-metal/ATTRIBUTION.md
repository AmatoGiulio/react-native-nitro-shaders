# Attribution — liquid-metal reference

`paper-design-reference.ts` is copied unmodified from
https://github.com/paper-design/shaders
(`packages/shaders/src/shaders/liquid-metal.ts`), licensed under the
Apache License, Version 2.0. Copyright paper-design.

Our `liquidMetal` material (AGSL/Metal) is a derivative work: the GLSL
fragment logic was ported to AGSL and MSL, with these changes:
- image/logo mask path removed (procedural shapes only in this port)
- GPU derivatives (fwidth/dFdx/dFdy) replaced by resolution-based
  analytic approximations (AGSL has no derivative functions)
- GLSL ES 3.0 syntax adapted to AGSL / MSL respectively

The simplex noise (`snoise`) implementation comes from the same
repository (`packages/shaders/src/shader-utils.ts`).
