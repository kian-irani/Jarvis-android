package com.kianirani.jarvis.ui.screen.hud

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.kianirani.jarvis.ui.theme.*
import kotlin.math.*

data class NodeInfo(val id:String, val name:String, val ip:String, val role:String, val online:Boolean, val cpu:Int, val ram:Int, val ping:Int)
enum class EventLevel { OK, INFO, WARN, ERR }
data class LogEvent(val time:String, val message:String, val level:EventLevel)
data class HudUiState(
    val nodes:List<NodeInfo>=emptyList(), val brainOnline:Boolean=false,
    val nodesOnline:Int=0, val groqOnline:Boolean=false,
    val brainCpu:Float=0f, val brainRam:Float=0f, val brainNet:Float=0f, val brainDiskIo:Float=0f,
    val jarvisOutput:String="", val inputText:String="", val isListening:Boolean=false,
    val waveformAmplitudes:List<Float>=List(40){0.05f}, val currentTime:String="",
    val eventLog:List<LogEvent>=emptyList(),
)

@Composable
fun HudScreen(viewModel: HudViewModel) {
    val state by viewModel.uiState.collectAsState()
    val wide = LocalConfiguration.current.screenWidthDp > 600
    Box(Modifier.fillMaxSize().background(JarvisColors.Background)) {
        HexGrid(Modifier.fillMaxSize())
        ScanLine(Modifier.fillMaxSize())
        if (wide) LandscapeLayout(state,viewModel) else PortraitLayout(state,viewModel)
        CornerBrackets(Modifier.fillMaxSize())
    }
}

@Composable fun PortraitLayout(s:HudUiState,vm:HudViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar(s,vm,Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().weight(1f).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(0.42f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                s.nodes.forEach { NodeCard(it,Modifier.fillMaxWidth()) }
                MetricPanel(s,Modifier.fillMaxWidth().weight(1f))
            }
            Column(Modifier.weight(0.58f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OrbPanel(s,Modifier.fillMaxWidth().weight(1f))
                TypewriterPanel(s.jarvisOutput,Modifier.fillMaxWidth())
                InputBar(s.inputText,vm::onInputChange,vm::sendChat,Modifier.fillMaxWidth())
            }
        }
        WaveformBar(s.waveformAmplitudes,s.isListening,Modifier.fillMaxWidth().height(60.dp))
    }
}

@Composable fun LandscapeLayout(s:HudUiState,vm:HudViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar(s,vm,Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().weight(1f).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(200.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                s.nodes.forEach { NodeCard(it,Modifier.fillMaxWidth()) }
                MetricPanel(s,Modifier.fillMaxWidth().weight(1f))
            }
            Column(Modifier.weight(1f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OrbPanel(s,Modifier.fillMaxWidth().weight(1f))
                TypewriterPanel(s.jarvisOutput,Modifier.fillMaxWidth())
                InputBar(s.inputText,vm::onInputChange,vm::sendChat,Modifier.fillMaxWidth())
            }
            Column(Modifier.width(200.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                RadarPanel(s,Modifier.fillMaxWidth().height(160.dp))
                LogPanel(s,Modifier.fillMaxWidth().weight(1f))
            }
        }
        WaveformBar(s.waveformAmplitudes,s.isListening,Modifier.fillMaxWidth().height(56.dp))
    }
}

@Composable fun TopBar(s:HudUiState,vm:HudViewModel,modifier:Modifier) {
    Row(modifier.height(52.dp)
        .background(Brush.horizontalGradient(listOf(JarvisColors.CyanFaint,Color.Transparent,JarvisColors.CyanFaint)))
        .padding(horizontal=14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.linearGradient(listOf(JarvisColors.BlueAccent,JarvisColors.BlueDeep)))
                .border(1.dp,JarvisColors.CyanSecondary,RoundedCornerShape(4.dp)),Alignment.Center) {
                Text("J",color=JarvisColors.CyanPrimary,style=MaterialTheme.typography.headlineMedium)
            }
            Column {
                Text("JARVIS",style=MaterialTheme.typography.headlineLarge,color=JarvisColors.CyanPrimary)
                Text("v4.1.0 — 3 NODES",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("BRAIN" to s.brainOnline,"NODES" to (s.nodesOnline>0),"GROQ" to s.groqOnline).forEach{(l,ok)->
                val c=if(ok)JarvisColors.NeonGreen else JarvisColors.DangerRed
                Column(Modifier.border(1.dp,c.copy(.3f),RoundedCornerShape(3.dp)).background(c.copy(.05f),RoundedCornerShape(3.dp)).padding(horizontal=8.dp,vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(l,style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
                    Text(if(ok)"OK" else "ERR",style=MaterialTheme.typography.labelSmall,color=c)
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(if(s.isListening)JarvisColors.CyanFaint else Color.Transparent)
                .border(1.dp,if(s.isListening)JarvisColors.CyanPrimary else JarvisColors.Border,RoundedCornerShape(3.dp))
                .clickable(onClick=vm::toggleListening).padding(horizontal=10.dp,vertical=4.dp)){
                Text(if(s.isListening)"◉ MIC" else "○ MIC",style=MaterialTheme.typography.labelMedium,color=if(s.isListening)JarvisColors.CyanPrimary else JarvisColors.TextDim)
            }
            Column(horizontalAlignment=Alignment.End){
                Text(s.currentTime,style=MaterialTheme.typography.headlineMedium,color=JarvisColors.CyanPrimary)
                Text("UTC+3:30",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
            }
        }
    }
}

@Composable fun OrbPanel(s:HudUiState,modifier:Modifier) {
    val inf=rememberInfiniteTransition(label="orb")
    val ring  by inf.animateFloat(0f,360f,infiniteRepeatable(tween(8000,easing=LinearEasing)),label="r1")
    val ring2 by inf.animateFloat(360f,0f,infiniteRepeatable(tween(13000,easing=LinearEasing)),label="r2")
    val pulse by inf.animateFloat(0.94f,1.06f,infiniteRepeatable(tween(2500,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="p")
    val glow  by inf.animateFloat(0.3f,0.65f,infiniteRepeatable(tween(2500,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="g")
    val blink by inf.animateFloat(0f,1f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="b")
    HudCard(modifier) {
        Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            val cx=size.width/2;val cy=size.height/2;val maxR=minOf(cx,cy)
            repeat(4){i->drawCircle(JarvisColors.GridLine,maxR*(0.28f+i*0.18f),Offset(cx,cy),style=Stroke(0.8.dp.toPx()))}
            rotate(ring,Offset(cx,cy)){
                drawCircle(JarvisColors.CyanSecondary.copy(.4f),maxR*.88f,Offset(cx,cy),style=Stroke(1.dp.toPx()))
                drawCircle(JarvisColors.CyanPrimary,4.dp.toPx(),Offset(cx+maxR*.88f,cy))
                drawCircle(JarvisColors.CyanGlow,8.dp.toPx(),Offset(cx+maxR*.88f,cy))
            }
            rotate(ring2,Offset(cx,cy)){
                drawCircle(JarvisColors.Border.copy(.5f),maxR*.65f,Offset(cx,cy),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(10f,8f),0f)))
                repeat(3){i->val a=(i*120f)*(PI/180f).toFloat();val px=cx+maxR*.65f*cos(a);val py=cy+maxR*.65f*sin(a)
                    drawCircle(JarvisColors.BlueDeep,3.dp.toPx(),Offset(px,py));drawCircle(JarvisColors.CyanGlow,5.dp.toPx(),Offset(px,py))}
            }
            val r=maxR*0.26f*pulse
            drawCircle(Brush.radialGradient(listOf(JarvisColors.CyanSecondary,JarvisColors.BlueDeep,JarvisColors.BlueMid),Offset(cx-r*.2f,cy-r*.2f),r),r,Offset(cx,cy))
            drawCircle(JarvisColors.CyanPrimary.copy(glow),r+14.dp.toPx(),Offset(cx,cy),style=Stroke(2.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(JarvisColors.CyanGlow.copy(glow*.5f),Color.Transparent),Offset(cx,cy),r*2.5f),r*2.5f,Offset(cx,cy))
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top=10.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("JARVIS",style=MaterialTheme.typography.displayLarge,color=JarvisColors.CyanPrimary)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text(if(s.isListening)"◉  LISTENING" else "●  STANDBY",style=MaterialTheme.typography.labelMedium,
                color=if(s.isListening)JarvisColors.NeonGreen.copy(.5f+blink*.5f) else JarvisColors.CyanSecondary.copy(.7f))
        }
    }
}

@Composable fun HudCard(modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Brush.linearGradient(listOf(Color(0xF5061525),Color(0xFB020B18)))).border(1.dp,JarvisColors.Border,RoundedCornerShape(4.dp))){
        BracketDeco(); content()
    }
}

@Composable fun BracketDeco(color:Color=JarvisColors.CyanSecondary,size:Dp=9.dp,sw:Dp=1.5.dp) {
    Canvas(Modifier.fillMaxSize()){
        val s=size.toPx();val p=4.dp.toPx();val w=sw.toPx();val W=this.size.width;val H=this.size.height
        listOf(Pair(Offset(p,p),Pair(Offset(p+s,p),Offset(p,p+s))),Pair(Offset(W-p,p),Pair(Offset(W-p-s,p),Offset(W-p,p+s))),
               Pair(Offset(p,H-p),Pair(Offset(p+s,H-p),Offset(p,H-p-s))),Pair(Offset(W-p,H-p),Pair(Offset(W-p-s,H-p),Offset(W-p,H-p-s)))).forEach{(o,e)->
            drawLine(color,o,e.first,w);drawLine(color,o,e.second,w)}
    }
}

@Composable fun TypewriterPanel(text:String,modifier:Modifier) {
    val cur by rememberInfiniteTransition(label="cur").animateFloat(0f,1f,infiniteRepeatable(tween(800),RepeatMode.Reverse),label="c")
    HudCard(modifier){Column(Modifier.padding(12.dp)){
        Text("JARVIS OUTPUT ▸",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=6.dp))
        Row{Text("◤ ",style=MaterialTheme.typography.bodyMedium,color=JarvisColors.CyanPrimary)
            Text(text,style=MaterialTheme.typography.bodyMedium,color=JarvisColors.TextPrimary,maxLines=3,overflow=TextOverflow.Ellipsis)
            Text("█",style=MaterialTheme.typography.bodyMedium,color=JarvisColors.CyanPrimary.copy(cur))}
    }}
}

@Composable fun NodeCard(n:NodeInfo,modifier:Modifier) {
    val inf=rememberInfiniteTransition(label="dot")
    val sc by inf.animateFloat(1f,2.5f,infiniteRepeatable(tween(1500,easing=FastOutSlowInEasing),RepeatMode.Restart),label="s")
    val col=if(n.online)JarvisColors.NeonGreen else JarvisColors.DangerRed
    HudCard(modifier){Column(Modifier.padding(10.dp)){
        Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Box(Modifier.size(12.dp),Alignment.Center){
                    Box(Modifier.size(10.dp).scale(sc).clip(CircleShape).background(col.copy(.25f)))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(col))
                }
                Text(n.name,style=MaterialTheme.typography.labelLarge,color=JarvisColors.TextPrimary)
            }
            Text(if(n.online)"ON" else "OFF",style=MaterialTheme.typography.labelSmall,color=col)
        }
        Spacer(Modifier.height(3.dp))
        Text(n.role,style=MaterialTheme.typography.bodySmall,color=JarvisColors.TextDim)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            listOf("CPU" to "${n.cpu}%","RAM" to "${n.ram}%").forEach{(l,v)->
                Column{Text(l,style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
                    Text(v,style=MaterialTheme.typography.titleLarge,color=JarvisColors.CyanPrimary)}
            }
        }
    }}
}

@Composable fun MetricPanel(s:HudUiState,modifier:Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){
        Text("BRAIN METRICS",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=8.dp))
        MetricBar("CPU",s.brainCpu,JarvisColors.CyanPrimary)
        MetricBar("RAM",s.brainRam,JarvisColors.BlueDeep)
        MetricBar("NET",s.brainNet,JarvisColors.NeonGreen)
    }}
}

@Composable fun MetricBar(label:String,value:Float,color:Color) {
    val v by animateFloatAsState(value,tween(400),label="bar")
    Column(Modifier.padding(bottom=7.dp)){
        Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){
            Text(label,style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
            Text("${v.toInt()}%",style=MaterialTheme.typography.labelSmall,color=color)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(JarvisColors.GridLine,RoundedCornerShape(1.dp))){
            Box(Modifier.fillMaxWidth((v/100f).coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(color.copy(.5f),color)),RoundedCornerShape(1.dp)))
        }
    }
}

@Composable fun RadarPanel(s:HudUiState,modifier:Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){
        Text("RADAR",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=4.dp))
        Canvas(Modifier.fillMaxSize()){
            val cx=size.width/2;val cy=size.height/2;val r=minOf(cx,cy)*.72f
            val vals=listOf(s.brainCpu/100f,s.brainRam/100f,s.brainNet/100f,0.45f,0.62f)
            val n=vals.size;val step=(2*PI/n).toFloat()
            repeat(4){i->val gr=r*((i+1)/4f);val path=Path()
                repeat(n){j->val a=step*j-(PI/2).toFloat();val px=cx+gr*cos(a);val py=cy+gr*sin(a)
                    if(j==0)path.moveTo(px,py) else path.lineTo(px,py)};path.close()
                drawPath(path,JarvisColors.GridLine,style=Stroke(.8.dp.toPx()))}
            repeat(n){j->val a=step*j-(PI/2).toFloat();drawLine(JarvisColors.GridLine,Offset(cx,cy),Offset(cx+r*cos(a),cy+r*sin(a)),.7.dp.toPx())}
            val dp=Path();vals.forEachIndexed{j,v->val a=step*j-(PI/2).toFloat();val px=cx+r*v*cos(a);val py=cy+r*v*sin(a)
                if(j==0)dp.moveTo(px,py) else dp.lineTo(px,py)};dp.close()
            drawPath(dp,JarvisColors.CyanFaint);drawPath(dp,JarvisColors.CyanPrimary,style=Stroke(1.5.dp.toPx()))
            vals.forEachIndexed{j,v->val a=step*j-(PI/2).toFloat();val px=cx+r*v*cos(a);val py=cy+r*v*sin(a)
                drawCircle(JarvisColors.CyanPrimary,3.dp.toPx(),Offset(px,py));drawCircle(JarvisColors.CyanGlow,6.dp.toPx(),Offset(px,py))}
        }
    }}
}

@Composable fun LogPanel(s:HudUiState,modifier:Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){
        Text("EVENT LOG",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=6.dp))
        s.eventLog.takeLast(8).forEach{e->
            Row(Modifier.padding(bottom=3.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                Text(e.time,style=MaterialTheme.typography.bodySmall,color=JarvisColors.TextDim)
                Text(e.message,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis,
                    color=when(e.level){EventLevel.OK->JarvisColors.NeonGreen;EventLevel.WARN->JarvisColors.WarningAmber;EventLevel.ERR->JarvisColors.DangerRed;else->JarvisColors.CyanSecondary})
            }
        }
    }}
}

@Composable fun InputBar(value:String,onChange:(String)->Unit,onSend:()->Unit,modifier:Modifier) {
    Row(modifier.border(1.dp,JarvisColors.Border,RoundedCornerShape(4.dp)).background(JarvisColors.CyanFaint,RoundedCornerShape(4.dp)).padding(horizontal=12.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Text("▶",color=JarvisColors.CyanPrimary,style=MaterialTheme.typography.bodyLarge)
        BasicTextField(value,onChange,Modifier.weight(1f),textStyle=MaterialTheme.typography.bodyMedium.copy(color=JarvisColors.TextPrimary),cursorBrush=SolidColor(JarvisColors.CyanPrimary),
            decorationBox={inner->Box{if(value.isEmpty())Text("Send command to JARVIS...",style=MaterialTheme.typography.bodyMedium,color=JarvisColors.TextDim);inner()}})
        Text("↵",style=MaterialTheme.typography.labelMedium,color=JarvisColors.TextDim)
    }
}

@Composable fun WaveformBar(amps:List<Float>,active:Boolean,modifier:Modifier) {
    Row(modifier.background(Brush.horizontalGradient(listOf(JarvisColors.CyanFaint,Color.Transparent,JarvisColors.CyanFaint))).padding(horizontal=14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text("VOICE",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
        Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical=6.dp)){
            val bw=(size.width-(amps.size-1)*3.dp.toPx())/amps.size;val mh=size.height
            amps.forEachIndexed{i,a->val bh=mh*a.coerceIn(.02f,1f);val x=i*(bw+3.dp.toPx());val y=(mh-bh)/2f
                drawRect(Brush.verticalGradient(if(active)listOf(JarvisColors.CyanPrimary,JarvisColors.BlueDeep) else listOf(JarvisColors.CyanSecondary.copy(.3f),JarvisColors.BlueDeep.copy(.2f)),startY=y,endY=y+bh),Offset(x,y),Size(bw,bh))}
        }
        Text(if(active)"ON" else "48k",style=MaterialTheme.typography.labelSmall,color=if(active)JarvisColors.NeonGreen else JarvisColors.TextDim)
    }
}

@Composable fun HexGrid(modifier:Modifier) {
    Canvas(modifier){
        val hw=52.dp.toPx();val hh=60.dp.toPx()
        val paint=Paint().apply{color=JarvisColors.GridLine;style=PaintingStyle.Stroke;strokeWidth=.8.dp.toPx()}
        repeat((size.height/hh).toInt()+2){row->repeat((size.width/hw).toInt()+2){col->
            val cx=col*hw+(if(row%2==1)hw/2 else 0f);val cy=row*hh*.75f
            val path=Path();repeat(6){i->val a=(60*i-30)*(PI/180f).toFloat()
                val px=cx+(hw/2)*cos(a);val py=cy+(hw/2)*sin(a)
                if(i==0)path.moveTo(px,py) else path.lineTo(px,py)};path.close()
            drawContext.canvas.drawPath(path,paint)}}
    }
}

@Composable fun ScanLine(modifier:Modifier) {
    val inf=rememberInfiniteTransition(label="scan")
    val y by inf.animateFloat(-5f,105f,infiniteRepeatable(tween(4000,easing=LinearEasing)),label="sy")
    Canvas(modifier.alpha(.3f)){
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent,JarvisColors.CyanPrimary,Color.Transparent)),Offset(0f,size.height*y/100f),Size(size.width,3.dp.toPx()))
    }
}

@Composable fun CornerBrackets(modifier:Modifier) {
    Canvas(modifier){
        val s=44.dp.toPx();val w=2.dp.toPx();val c=JarvisColors.CyanSecondary.copy(.45f);val W=size.width;val H=size.height
        listOf(Pair(Offset(0f,0f),Pair(Offset(s,0f),Offset(0f,s))),Pair(Offset(W,0f),Pair(Offset(W-s,0f),Offset(W,s))),
               Pair(Offset(0f,H),Pair(Offset(s,H),Offset(0f,H-s))),Pair(Offset(W,H),Pair(Offset(W-s,H),Offset(W,H-s)))).forEach{(o,e)->
            drawLine(c,o,e.first,w);drawLine(c,o,e.second,w)}
    }
}
