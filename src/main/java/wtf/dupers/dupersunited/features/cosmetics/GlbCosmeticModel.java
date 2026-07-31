package wtf.dupers.dupersunited.features.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wtf.dupers.dupersunited.MainClient;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GlbCosmeticModel {
    private static final GlbCosmeticModel EMPTY=new GlbCosmeticModel("empty",List.of(),null);
    private final String name;
    private final List<Primitive> primitives;
    private final byte[] texture;
    private Identifier textureId;

    private GlbCosmeticModel(String name,List<Primitive> primitives,byte[] texture){this.name=name;this.primitives=primitives;this.texture=texture;}

    public static GlbCosmeticModel load(String name){
        try(InputStream in=GlbCosmeticModel.class.getResourceAsStream("/assets/dupersunited/models/cosmetics/"+name)){
            if(in==null)throw new IllegalStateException("Missing "+name);
            byte[] bytes=in.readAllBytes();ByteBuffer glb=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(glb.getInt()!=0x46546C67)throw new IllegalArgumentException("Invalid GLB");glb.getInt();glb.getInt();
            int jsonLength=glb.getInt();glb.getInt();byte[] jsonBytes=new byte[jsonLength];glb.get(jsonBytes);
            JsonObject json=JsonParser.parseString(new String(jsonBytes,StandardCharsets.UTF_8).trim()).getAsJsonObject();
            int binLength=glb.getInt();glb.getInt();byte[] binary=new byte[binLength];glb.get(binary);
            return parse(name,json,ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN));
        }catch(Exception e){MainClient.LOGGER.error("Failed to load cosmetic GLB {}",name,e);return EMPTY;}
    }

    private static GlbCosmeticModel parse(String name,JsonObject json,ByteBuffer binary){
        List<Primitive> out=new ArrayList<>();JsonObject scene=json.getAsJsonArray("scenes").get(json.has("scene")?json.get("scene").getAsInt():0).getAsJsonObject();for(var root:scene.getAsJsonArray("nodes"))loadNode(json,binary,root.getAsInt(),new Matrix4f(),out);normalize(out);
        byte[] image=null;if(json.has("images")){JsonObject im=json.getAsJsonArray("images").get(0).getAsJsonObject();JsonObject view=json.getAsJsonArray("bufferViews").get(im.get("bufferView").getAsInt()).getAsJsonObject();int start=view.has("byteOffset")?view.get("byteOffset").getAsInt():0,length=view.get("byteLength").getAsInt();image=new byte[length];ByteBuffer copy=binary.duplicate();copy.position(start);copy.get(image);}
        return new GlbCosmeticModel(name,List.copyOf(out),image);
    }

    private static void loadNode(JsonObject json,ByteBuffer binary,int index,Matrix4f parent,List<Primitive> out){JsonObject node=json.getAsJsonArray("nodes").get(index).getAsJsonObject();Matrix4f world=new Matrix4f(parent).mul(transform(node));if(node.has("mesh"))loadMesh(json,binary,node.get("mesh").getAsInt(),world,out);if(node.has("children"))for(var child:node.getAsJsonArray("children"))loadNode(json,binary,child.getAsInt(),world,out);}

    private static void loadMesh(JsonObject json,ByteBuffer binary,int meshIndex,Matrix4f transform,List<Primitive> out){
        for(var primitiveElement:json.getAsJsonArray("meshes").get(meshIndex).getAsJsonObject().getAsJsonArray("primitives")){
            JsonObject primitive=primitiveElement.getAsJsonObject(),attributes=primitive.getAsJsonObject("attributes");
            float[] positions=floats(json,binary,attributes.get("POSITION").getAsInt(),3);
            float[] normals=attributes.has("NORMAL")?floats(json,binary,attributes.get("NORMAL").getAsInt(),3):new float[positions.length];
            float[] uv=attributes.has("TEXCOORD_0")?floats(json,binary,attributes.get("TEXCOORD_0").getAsInt(),2):new float[positions.length/3*2];
            transform(positions,normals,transform);int[] indices=indices(json,binary,primitive.get("indices").getAsInt());out.add(new Primitive(positions,normals,uv,indices));
        }
    }

    private static Matrix4f transform(JsonObject node){if(node.has("matrix")){float[] m=new float[16];for(int i=0;i<16;i++)m[i]=node.getAsJsonArray("matrix").get(i).getAsFloat();return new Matrix4f().set(m);}Vector3f t=new Vector3f(),s=new Vector3f(1);Quaternionf r=new Quaternionf();if(node.has("translation")){var a=node.getAsJsonArray("translation");t.set(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}if(node.has("scale")){var a=node.getAsJsonArray("scale");s.set(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}if(node.has("rotation")){var a=node.getAsJsonArray("rotation");r.set(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat());}return new Matrix4f().translationRotateScale(t,r,s);}

    private static void transform(float[] positions,float[] normals,Matrix4f matrix){Matrix3f normalMatrix=new Matrix3f(matrix).invert().transpose();Vector3f v=new Vector3f();for(int i=0;i<positions.length;i+=3){matrix.transformPosition(v.set(positions[i],positions[i+1],positions[i+2]));positions[i]=v.x;positions[i+1]=v.y;positions[i+2]=v.z;normalMatrix.transform(v.set(normals[i],normals[i+1],normals[i+2])).normalize();normals[i]=v.x;normals[i+1]=v.y;normals[i+2]=v.z;}}

    private static void normalize(List<Primitive> primitives){float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;for(Primitive p:primitives)for(int i=0;i<p.positions.length;i+=3){minX=Math.min(minX,p.positions[i]);minY=Math.min(minY,p.positions[i+1]);minZ=Math.min(minZ,p.positions[i+2]);maxX=Math.max(maxX,p.positions[i]);maxZ=Math.max(maxZ,p.positions[i+2]);}float cx=(minX+maxX)/2,cz=(minZ+maxZ)/2;for(Primitive p:primitives)for(int i=0;i<p.positions.length;i+=3){p.positions[i]-=cx;p.positions[i+1]-=minY;p.positions[i+2]-=cz;}}

    private static float[] floats(JsonObject json,ByteBuffer binary,int accessorIndex,int components){
        JsonObject accessor=json.getAsJsonArray("accessors").get(accessorIndex).getAsJsonObject(),view=json.getAsJsonArray("bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        int count=accessor.get("count").getAsInt(),start=(view.has("byteOffset")?view.get("byteOffset").getAsInt():0)+(accessor.has("byteOffset")?accessor.get("byteOffset").getAsInt():0),stride=view.has("byteStride")?view.get("byteStride").getAsInt():components*4;float[] out=new float[count*components];
        for(int i=0;i<count;i++)for(int c=0;c<components;c++)out[i*components+c]=binary.getFloat(start+i*stride+c*4);return out;
    }

    private static int[] indices(JsonObject json,ByteBuffer binary,int accessorIndex){
        JsonObject accessor=json.getAsJsonArray("accessors").get(accessorIndex).getAsJsonObject(),view=json.getAsJsonArray("bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();int count=accessor.get("count").getAsInt(),type=accessor.get("componentType").getAsInt(),start=(view.has("byteOffset")?view.get("byteOffset").getAsInt():0)+(accessor.has("byteOffset")?accessor.get("byteOffset").getAsInt():0);int[] out=new int[count];for(int i=0;i<count;i++)out[i]=switch(type){case 5121->binary.get(start+i)&255;case 5123->binary.getShort(start+i*2)&65535;case 5125->binary.getInt(start+i*4);default->throw new IllegalArgumentException("Index type "+type);};return out;
    }

    public void render(MatrixStack matrices,OrderedRenderCommandQueue queue,int light){
        if(primitives.isEmpty())return;prepareTexture();queue.submitCustom(matrices,RenderLayers.entityCutoutNoCull(textureId),(entry,vertices)->primitives.forEach(p->p.render(entry,vertices,light)));
    }

    private void prepareTexture(){if(textureId!=null)return;textureId=Identifier.of("dupersunited","cosmetics/"+name.replace('.','_'));try{NativeImage image=texture==null?new NativeImage(1,1,false):NativeImage.read(texture);if(texture==null)image.setColorArgb(0,0,0xFFFFFFFF);MinecraftClient.getInstance().getTextureManager().registerTexture(textureId,new NativeImageBackedTexture(()->"DupersUnited cosmetic "+name,image));}catch(Exception e){throw new IllegalStateException(e);}}

    private record Primitive(float[] positions,float[] normals,float[] uv,int[] indices){
        private void render(MatrixStack.Entry entry,VertexConsumer vertices,int light){for(int i=0;i<indices.length;i+=3){vertex(entry,vertices,light,indices[i]);vertex(entry,vertices,light,indices[i+1]);vertex(entry,vertices,light,indices[i+2]);vertex(entry,vertices,light,indices[i+2]);}}
        private void vertex(MatrixStack.Entry entry,VertexConsumer vertices,int light,int index){int p=index*3,t=index*2;vertices.vertex(entry,positions[p],positions[p+1],positions[p+2]).color(0xFFFFFFFF).texture(uv[t],uv[t+1]).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry,normals[p],normals[p+1],normals[p+2]);}
    }
}
