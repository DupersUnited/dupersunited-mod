package wtf.dupers.dupersunited.features.screens;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticCatalog;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticsFeatureRenderer;
import wtf.dupers.dupersunited.modules.render.CosmeticsModule;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.utils.ColorUtil;

import java.util.List;

public final class CosmeticsPickerScreen extends Screen {
    private final CosmeticsModule module;
    private CosmeticCatalog.Slot slot = CosmeticCatalog.Slot.HEAD;
    private String query = "";
    private int scroll;
    private int controlScroll;
    private float previewYaw,previewPitch,previewZoom=1,previewPanX,previewPanY;

    public CosmeticsPickerScreen(CosmeticsModule module) { super(Text.literal("Cosmetics")); this.module = module; }

    public static void openSource(CosmeticCatalog.Item item) { if (!item.source().isBlank()) Util.getOperatingSystem().open(item.source()); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w=Math.min(360,width-30), x=(width-w)/2, y=16, h=height-32;
        ctx.fill(x,y,x+w,y+h,ColorUtil.DEEP_SAPPHIRE);
        ctx.drawCenteredTextWithShadow(textRenderer,"Cosmetics",width/2,y+8,ColorUtil.MAUVE);
        button(ctx,x+10,y+24,(w-24)/2,15,"Head",slot==CosmeticCatalog.Slot.HEAD,mouseX,mouseY);
        button(ctx,x+14+(w-24)/2,y+24,(w-24)/2,15,"Tail",slot==CosmeticCatalog.Slot.TAIL,mouseX,mouseY);
        ctx.fill(x+10,y+44,x+w-10,y+59,ColorUtil.DEEP_INDIGO);
        ctx.drawText(textRenderer,query.isEmpty()?"Search...":query,x+14,y+48,query.isEmpty()?ColorUtil.SUBTEXT:ColorUtil.PALE_NAVY,false);
        int listRight=x+w-145;
        List<CosmeticCatalog.Item> list=CosmeticCatalog.search(slot,query);
        int rows=Math.max(1,(h-105)/18), max=Math.max(0,list.size()-rows); scroll=Math.max(0,Math.min(scroll,max));
        for(int i=scroll;i<Math.min(list.size(),scroll+rows);i++){
            CosmeticCatalog.Item item=list.get(i); int ry=y+65+(i-scroll)*18; boolean selected=selected().equals(item.name());
            boolean hover=mouseX>=x+10&&mouseX<listRight&&mouseY>=ry&&mouseY<ry+16;
            ctx.fill(x+10,ry,listRight,ry+16,hover?0x4439C5BB:selected?0x5549B8A8:ColorUtil.DEEP_INDIGO);
            ctx.drawText(textRenderer,item.name(),x+15,ry+4,selected?ColorUtil.TEAL:ColorUtil.PALE_NAVY,false);
            String creator=item.creator(); int available=listRight-x-30-textRenderer.getWidth(item.name()); if(available>textRenderer.getWidth(creator))ctx.drawText(textRenderer,creator,listRight-5-textRenderer.getWidth(creator),ry+4,ColorUtil.SUBTEXT,false);
        }
        int px=listRight+5,py=y+65,pw=x+w-10-px,ph=Math.max(90,h-325);
        ctx.fill(px,py,px+pw,py+ph,0xFF111522);
        ctx.enableScissor(px,py,px+pw,py+ph);
        if(client!=null&&client.player!=null)drawPreview(ctx,px,py,pw,ph);
        ctx.disableScissor();
        int controlTop=py+ph+5,controlBottom=y+h-38,maxControlScroll=Math.max(0,189-Math.max(0,controlBottom-controlTop));controlScroll=Math.max(0,Math.min(controlScroll,maxControlScroll));
        ctx.enableScissor(px,controlTop,px+pw,controlBottom);drawControls(ctx,px,controlTop-controlScroll,pw,mouseX,mouseY);ctx.disableScissor();
        CosmeticCatalog.Item item=CosmeticCatalog.named(slot,selected());
        String credit="By "+item.creator()+(item.source().isBlank()?"":" - click to open source");
        ctx.drawCenteredTextWithShadow(textRenderer,credit,width/2,y+h-28,ColorUtil.SUBTEXT);
        super.render(ctx,mouseX,mouseY,delta);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void drawPreview(DrawContext ctx,int x,int y,int w,int h){
        EntityRenderer renderer=client.getEntityRenderDispatcher().getRenderer(client.player);
        EntityRenderState state=renderer.getAndUpdateRenderState(client.player,1f);
        if(state instanceof LivingEntityRenderState living){living.bodyYaw=180+previewYaw;living.relativeHeadYaw=0;living.pitch=previewPitch;living.width/=living.baseScale;living.height/=living.baseScale;living.baseScale=1;}
        state.light=15728880;state.shadowPieces.clear();state.outlineColor=0;
        float size=42*previewZoom;Vector3f offset=new Vector3f(previewPanX/size,state.height/2+previewPanY/size,0);
        CosmeticsFeatureRenderer.preview(()->ctx.addEntity(state,size,offset,new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),x,y,x+w,y+h));
    }

    private void drawControls(DrawContext ctx,int x,int y,int w,int mx,int my){
        ctx.drawCenteredTextWithShadow(textRenderer,slot==CosmeticCatalog.Slot.HEAD?"Head position":"Tail position",x+w/2,y,ColorUtil.MAUVE);
        IntSetting[] values=settings();String[] names={"X","Y","Z","Size","Width","Height","Pitch","Yaw","Roll"};
        for(int i=0;i<9;i++){int ry=y+13+i*16;ctx.fill(x,ry,x+w,ry+14,ColorUtil.DEEP_INDIGO);ctx.drawText(textRenderer,names[i],x+4,ry+3,ColorUtil.PALE_NAVY,false);ctx.drawText(textRenderer,"-",x+w-43,ry+3,ColorUtil.TEAL,false);String value=Integer.toString(values[i].getValue());ctx.drawText(textRenderer,value,x+w-24-textRenderer.getWidth(value)/2,ry+3,ColorUtil.PALE_NAVY,false);ctx.drawText(textRenderer,"+",x+w-8,ry+3,ColorUtil.TEAL,false);}
        int resetY=y+158;button(ctx,x,resetY,w,14,"Reset",false,mx,my);button(ctx,x,resetY+17,w,14,"Animate: "+module.animate.getValue(),module.animate.getValue(),mx,my);
    }

    private IntSetting[] settings(){return slot==CosmeticCatalog.Slot.HEAD?new IntSetting[]{module.headX,module.headY,module.headZ,module.headSize,module.headWidth,module.headStretch,module.headPitch,module.headYaw,module.headRoll}:new IntSetting[]{module.tailX,module.tailY,module.tailZ,module.tailSize,module.tailWidth,module.tailStretch,module.tailPitch,module.tailYaw,module.tailRoll};}

    private void button(DrawContext c,int x,int y,int w,int h,String text,boolean on,int mx,int my){ c.fill(x,y,x+w,y+h,on?0x6649B8A8:(mx>=x&&mx<x+w&&my>=y&&my<y+h?0x4439C5BB:ColorUtil.DEEP_INDIGO)); c.drawCenteredTextWithShadow(textRenderer,text,x+w/2,y+4,on?ColorUtil.TEAL:ColorUtil.PALE_NAVY); }
    private String selected(){ return slot==CosmeticCatalog.Slot.HEAD?module.head.getValue():module.tail.getValue(); }
    private void select(String value){ if(slot==CosmeticCatalog.Slot.HEAD)module.head.setValue(value);else module.tail.setValue(value);module.setEnabled(!module.head.getValue().equals("None")||!module.tail.getValue().equals("None"));ConfigManager.save(); }

    @Override public boolean mouseClicked(Click click,boolean doubled){ int mx=(int)click.x(),my=(int)click.y(),w=Math.min(360,width-30),x=(width-w)/2,y=16,h=height-32;
        if(my>=y+24&&my<y+39){ slot=mx<x+w/2?CosmeticCatalog.Slot.HEAD:CosmeticCatalog.Slot.TAIL;scroll=0;controlScroll=0;return true; }
        List<CosmeticCatalog.Item> list=CosmeticCatalog.search(slot,query); int rows=Math.max(1,(h-105)/18);
        int listRight=x+w-145;
        for(int i=scroll;i<Math.min(list.size(),scroll+rows);i++){int ry=y+65+(i-scroll)*18;if(mx>=x+10&&mx<listRight&&my>=ry&&my<ry+16){select(list.get(i).name());return true;}}
        int previewH=Math.max(90,h-325),controlTop=y+65+previewH+5,controlBottom=y+h-38,maxControlScroll=Math.max(0,189-Math.max(0,controlBottom-controlTop)),controlY=controlTop-Math.min(controlScroll,maxControlScroll);
        int panelX=x+w-140,panelW=130;IntSetting[] values=settings();for(int i=0;i<9;i++){int ry=controlY+13+i*16;if(my>=controlTop&&my<controlBottom&&my>=ry&&my<ry+14){int step=i<3?1:5;if(mx>=panelX+panelW-48&&mx<panelX+panelW-30)values[i].setValue(Math.max(values[i].getMin(),values[i].getValue()-step));else if(mx>=panelX+panelW-18&&mx<panelX+panelW)values[i].setValue(Math.min(values[i].getMax(),values[i].getValue()+step));ConfigManager.save();return true;}}
        int resetY=controlY+158;if(my>=controlTop&&my<controlBottom&&mx>=panelX&&mx<panelX+panelW&&my>=resetY&&my<resetY+14){for(int i=0;i<values.length;i++)values[i].setValue(i>=3&&i<=5?100:0);ConfigManager.save();return true;}if(my>=controlTop&&my<controlBottom&&mx>=panelX&&mx<panelX+panelW&&my>=resetY+17&&my<resetY+31){module.animate.toggle();ConfigManager.save();return true;}
        if(my>=y+h-34&&my<y+h-18){openSource(CosmeticCatalog.named(slot,selected()));return true;} return super.mouseClicked(click,doubled); }
    @Override public boolean mouseDragged(Click click,double dx,double dy){int w=Math.min(360,width-30),x=(width-w)/2;if(click.x()>x+w-145){if(click.button()==GLFW.GLFW_MOUSE_BUTTON_LEFT){previewYaw=(previewYaw-(float)dx*1.5f)%360;previewPitch=(float)Math.max(-80,Math.min(80,previewPitch+dy));}else if(click.button()==GLFW.GLFW_MOUSE_BUTTON_RIGHT){previewPanX+=(float)dx;previewPanY+=(float)dy;}return true;}return super.mouseDragged(click,dx,dy);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){int w=Math.min(360,width-30),x=(width-w)/2,y=16,h=height-32,previewH=Math.max(90,h-325),controlTop=y+70+previewH,controlBottom=y+h-38,max=Math.max(0,189-Math.max(0,controlBottom-controlTop));if(mx>x+w-145&&my>=controlTop&&max>0){controlScroll=Math.max(0,Math.min(max,controlScroll-(int)Math.signum(vertical)*12));}else if(mx>x+w-145){previewZoom=Math.max(.55f,Math.min(1.8f,previewZoom+(float)vertical*.1f));}else scroll=Math.max(0,scroll-(int)Math.signum(vertical));return true;}
    @Override public boolean charTyped(CharInput input){if(input.isValidChar()){query+=input.asString();scroll=0;return true;}return super.charTyped(input);}
    @Override public boolean keyPressed(KeyInput input){if(input.getKeycode()==GLFW.GLFW_KEY_BACKSPACE&&!query.isEmpty()){query=query.substring(0,query.length()-1);scroll=0;return true;}return super.keyPressed(input);}
    @Override public void close(){ConfigManager.save();super.close();}
    @Override public boolean shouldPause(){return false;}
}
