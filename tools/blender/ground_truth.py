import bpy, math, sys

env_path = sys.argv[sys.argv.index("--") + 1]
out_path = sys.argv[sys.argv.index("--") + 2]

# clean scene
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete()

# sphere
bpy.ops.mesh.primitive_uv_sphere_add(segments=128, ring_count=64, radius=1.0)
sphere = bpy.context.active_object
bpy.ops.object.shade_smooth()

# chrome material
mat = bpy.data.materials.new("Chrome")
mat.use_nodes = True
bsdf = mat.node_tree.nodes["Principled BSDF"]
bsdf.inputs["Metallic"].default_value = 1.0
bsdf.inputs["Roughness"].default_value = 0.05
bsdf.inputs["Base Color"].default_value = (0.9, 0.9, 0.9, 1.0)
sphere.data.materials.append(mat)

# world = our equirect env
world = bpy.data.worlds.new("EnvWorld")
world.use_nodes = True
nt = world.node_tree
bg = nt.nodes["Background"]
envtex = nt.nodes.new("ShaderNodeTexEnvironment")
envtex.image = bpy.data.images.load(env_path)
nt.links.new(envtex.outputs["Color"], bg.inputs["Color"])
bg.inputs["Strength"].default_value = 1.0
bpy.context.scene.world = world

# camera straight at the sphere
cam_data = bpy.data.cameras.new("Cam")
cam = bpy.data.objects.new("Cam", cam_data)
bpy.context.collection.objects.link(cam)
cam.location = (0, -4.2, 0)
cam.rotation_euler = (math.radians(90), 0, 0)
bpy.context.scene.camera = cam

# render settings
scene = bpy.context.scene
scene.render.engine = 'BLENDER_EEVEE_NEXT' if hasattr(bpy.types, 'RenderSettings') and 'BLENDER_EEVEE_NEXT' in [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items] else 'BLENDER_EEVEE'
scene.render.resolution_x = 800
scene.render.resolution_y = 800
scene.render.filepath = out_path
bpy.ops.render.render(write_still=True)
