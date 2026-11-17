package com.f1nkero.qingxu

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Mint = Color(0xFF77AC78)
private val Dark = Color(0xFF244B38)
private val Pale = Color(0xFFF1F8F0)
private data class Task(val id:String, val title:String, val start:String, val daily:Boolean, val streak:Boolean, val checked:Set<String>)

private fun load(raw:String):List<Task> = runCatching {
    val arr=JSONArray(raw)
    (0 until arr.length()).map { i ->
        val j=arr.getJSONObject(i); val c=j.optJSONArray("checked") ?: JSONArray()
        Task(j.getString("id"),j.getString("title"),j.getString("start"),j.optBoolean("daily",true),j.optBoolean("streak",false),(0 until c.length()).map{c.getString(it)}.toSet())
    }
}.getOrDefault(emptyList())

private fun save(tasks:List<Task>):String = JSONArray().apply {
    tasks.forEach { t -> put(JSONObject().apply {
        put("id",t.id);put("title",t.title);put("start",t.start);put("daily",t.daily);put("streak",t.streak)
        put("checked",JSONArray(t.checked.sorted()))
    }) }
}.toString()

private fun days(t:Task, today:LocalDate):Int {
    if(!t.daily || !t.streak) return 0
    var date=if(today.toString() in t.checked) today else today.minusDays(1)
    var count=0
    while(date.toString() in t.checked && !date.isBefore(LocalDate.parse(t.start))) {
        count++;date=date.minusDays(1)
    }
    return count
}

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent { QingXu() } }
}

@Composable
private fun QingXu() {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("qingxu_tasks",Activity.MODE_PRIVATE) }
    var tasks by remember { mutableStateOf(load(prefs.getString("tasks","[]") ?: "[]")) }
    fun update(next:List<Task>) { tasks=next; prefs.edit().putString("tasks",save(next)).apply() }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selected by remember { mutableStateOf(today) }
    fun refreshDate() {
        val actualToday = LocalDate.now()
        if (actualToday != today) {
            // Follow the new date if the user was viewing the previous "today".
            if (selected == today) selected = actualToday
            today = actualToday
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refreshDate()
            // Also refresh when the app remains open across midnight.
            delay(30_000L)
        }
    }
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var daily by remember { mutableStateOf(true) }
    var streak by remember { mutableStateOf(false) }
    val visible=tasks.filter { !selected.isBefore(LocalDate.parse(it.start)) && (it.daily || it.start==selected.toString()) }
    val done=visible.count { selected.toString() in it.checked }
    MaterialTheme(colorScheme=lightColorScheme(primary=Mint,background=Pale,surface=Color.White,onSurface=Dark)) {
        Scaffold(containerColor=Pale,bottomBar={
            NavigationBar(containerColor=Color.White) {
                listOf(Triple("今日待办",Icons.Rounded.CheckCircle,0),Triple("统计",Icons.Rounded.BarChart,1),Triple("设置",Icons.Rounded.Settings,2)).forEach { (label,icon,index) ->
                    NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(icon,null)},label={Text(label)})
                }
            }
        },floatingActionButton={
            if(tab==0) ExtendedFloatingActionButton(onClick={ editing=null; name="";daily=true;streak=false;adding=true },containerColor=Mint,contentColor=Color.White,text={Text("添加任务")},icon={Icon(Icons.Rounded.Add,null)})
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp)) {
                Spacer(Modifier.height(22.dp))
                Text(when(tab){0->"今日待办";1->"完成统计";else->"设置"},fontSize=30.sp,fontWeight=FontWeight.Bold,color=Dark)
                Text("专注当下 · 让每一天都有一点进步",color=Color.Gray,fontSize=13.sp)
                Spacer(Modifier.height(22.dp))
                when(tab) {
                    0 -> {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={selected=selected.minusDays(1)}){Icon(Icons.Rounded.ChevronLeft,null)}
                            Text(selected.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),modifier=Modifier.weight(1f),fontWeight=FontWeight.Medium)
                            TextButton(onClick={refreshDate(); selected=LocalDate.now()}){Text("今天")}
                            IconButton(onClick={selected=selected.plusDays(1)}){Icon(Icons.Rounded.ChevronRight,null)}
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) {
                                Text("已完成 $done / ${visible.size}",fontWeight=FontWeight.Bold,fontSize=18.sp)
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(progress={if(visible.isEmpty()) 0f else done.toFloat()/visible.size},modifier=Modifier.fillMaxWidth(),color=Mint,trackColor=Pale)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=100.dp)) {
                            items(visible,key={it.id}) { t ->
                                Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                        Checkbox(checked=selected.toString() in t.checked,onCheckedChange={checked->
                                            update(tasks.map { if(it.id==t.id) it.copy(checked=if(checked) it.checked+selected.toString() else it.checked-selected.toString()) else it })
                                        })
                                        Column(Modifier.weight(1f).clickable { editing=t;name=t.title;daily=t.daily;streak=t.streak;adding=true }) {
                                            Text(t.title,fontWeight=FontWeight.Medium)
                                            if(t.streak && t.daily) Text("连续坚持 ${days(t,today)} 天",color=Mint,fontSize=12.sp)
                                            else Text(if(t.daily) "每天" else "单次任务",color=Color.Gray,fontSize=12.sp)
                                        }
                                        IconButton(onClick={editing=t;name=t.title;daily=t.daily;streak=t.streak;adding=true}){Icon(Icons.Rounded.Edit,null,tint=Mint)}
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val week=(0..6).map { today.minusDays((6-it).toLong()) }
                        Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                Text("近 7 天完成情况",fontSize=19.sp,fontWeight=FontWeight.Bold)
                                Spacer(Modifier.height(18.dp))
                                week.forEach { d ->
                                    val list=tasks.filter { !d.isBefore(LocalDate.parse(it.start)) && (it.daily || it.start==d.toString()) }
                                    val completed=list.count { d.toString() in it.checked }
                                    Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                                        Text(d.format(DateTimeFormatter.ofPattern("MM/dd")),modifier=Modifier.width(65.dp))
                                        LinearProgressIndicator(progress={if(list.isEmpty()) 0f else completed.toFloat()/list.size},modifier=Modifier.weight(1f),color=Mint,trackColor=Pale)
                                        Text(" $completed/${list.size}",fontSize=12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("坚持中的习惯",fontWeight=FontWeight.Bold,fontSize=19.sp)
                        tasks.filter { it.streak && it.daily }.forEach { t ->
                            Text("${t.title} · 连续 ${days(t,today)} 天",modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
                        }
                    }
                    else -> {
                        Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(22.dp)) {
                                Text("青序 · QingXu",fontSize=22.sp,fontWeight=FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                Text("清爽地安排每天，轻松地养成习惯。")
                                Spacer(Modifier.height(12.dp))
                                Text("所有数据仅保存在本机。每项任务可独立选择是否统计连续坚持天数。",color=Color.Gray,fontSize=13.sp)
                            }
                        }
                    }
                }
            }
        }
        if(adding) AlertDialog(onDismissRequest={adding=false},title={Text(if(editing==null) "新建任务" else "编辑任务")},text={
            Column {
                OutlinedTextField(value=name,onValueChange={name=it},label={Text("任务内容")},singleLine=true)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("每天重复",modifier=Modifier.weight(1f))
                    Switch(checked=daily,onCheckedChange={daily=it;if(!it) streak=false})
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("统计连续坚持天数",modifier=Modifier.weight(1f))
                    Switch(checked=streak,onCheckedChange={streak=it},enabled=daily)
                }
                if(editing!=null) TextButton(onClick={update(tasks.filterNot{it.id==editing!!.id});adding=false}){Text("删除此任务",color=Color(0xFFB64E4E))}
            }
        },confirmButton={TextButton(enabled=name.isNotBlank(),onClick={
            val old=editing
            val item=Task(old?.id?:UUID.randomUUID().toString(),name.trim(),old?.start?:selected.toString(),daily,streak,old?.checked?:emptySet())
            update(if(old==null) tasks+item else tasks.map{if(it.id==old.id)item else it})
            adding=false
        }){Text("保存")}},dismissButton={TextButton(onClick={adding=false}){Text("取消")}})
    }
}
