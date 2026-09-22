package com.drabdie.tweak;

import android.app.*;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    final int BG=Color.rgb(11,13,20), CARD=Color.rgb(22,25,36), TEXT=Color.rgb(245,246,255), MUTED=Color.rgb(155,161,184), ACCENT=Color.rgb(124,131,253), GREEN=Color.rgb(67,211,145), ORANGE=Color.rgb(255,174,79);
    LinearLayout root, content; TextView title, subtitle, shizukuStatus; Handler handler=new Handler(Looper.getMainLooper()); int selected=0; String[] profiles={"Performance","Balanced","Battery","Game Mode","Custom"};
    TextView ramView, batView, cpuView, stoView; SharedPreferences prefs; AtomicBoolean cpuBusy=new AtomicBoolean(false); AtomicBoolean applying=new AtomicBoolean(false);
    boolean gameAutoActive=false;
    IShellService shell=null; Shizuku.UserServiceArgs shellArgs=null; boolean shellBound=false;
    ServiceConnection shellConn=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){shell=IShellService.Stub.asInterface(b);}public void onServiceDisconnected(ComponentName n){shell=null;shellBound=false;}};

    // ---------- tiny UI helpers (same style as v1.0) ----------
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);} TextView tv(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");return t;}
    GradientDrawable bg(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    TextView label(String s){TextView t=tv(s,12,MUTED);t.setAllCaps(true);t.setLetterSpacing(.08f);return t;}

    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("drab",0); selected=prefs.getInt("profile",0); shellArgs=new Shizuku.UserServiceArgs(new ComponentName(this,ShellService.class)).daemon(false).processNameSuffix("shell").debuggable(false).version(1); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); build(); handler.post(tick); try{Shizuku.addRequestPermissionResultListener((req,res)->updateShizuku());}catch(Throwable ignored){}}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);try{if(shellBound)Shizuku.unbindUserService(shellArgs,shellConn,false);}catch(Throwable ignored){}super.onDestroy();}

    void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG); root.setPadding(dp(20),dp(16),dp(20),0);
        LinearLayout head=row(); title=tv("DraB Tweak",27,TEXT); title.setTypeface(null,Typeface.BOLD); head.addView(title,new LinearLayout.LayoutParams(0,dp(42),1)); TextView more=tv("⋮",32,MUTED); more.setOnClickListener(v->about()); head.addView(more,new LinearLayout.LayoutParams(dp(38),dp(42))); root.addView(head);
        subtitle=tv("NO-ROOT SYSTEM MANAGER",11,ACCENT); subtitle.setLetterSpacing(.12f); root.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(24)));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL); ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(0,dp(10),0,dp(10));String[] ns={"⌂\nHome","◈\nMonitor","⚙\nTools"};for(int i=0;i<3;i++){final int x=i;TextView n=tv(ns[i],12,i==0?TEXT:MUTED);n.setGravity(Gravity.CENTER);n.setPadding(0,dp(5),0,dp(5));n.setOnClickListener(v->{if(x==0)showHome();else if(x==1)showMonitor();else showTools();});nav.addView(n,new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(nav);setContentView(root);showHome();}
    void clear(){content.removeAllViews();ramView=null;batView=null;cpuView=null;stoView=null;}
    TextView section(String s){TextView t=label(s);t.setPadding(0,dp(14),0,dp(8));content.addView(t);return t;}
    TextView button(String s){TextView b=tv(s,14,TEXT);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),0,dp(14),0);b.setBackground(bg(ACCENT,14));return b;}
    View card(String a,String b,String c){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(13),dp(16),dp(13));l.setBackground(bg(CARD,16));TextView x=tv(a,16,TEXT);x.setTypeface(null,Typeface.BOLD);l.addView(x);l.addView(tv(b,12,MUTED));l.addView(tv(c,11,ACCENT));return l;}

    // ---------- Home ----------
    void showHome(){clear();section("CURRENT PROFILE");LinearLayout hero=row();hero.setPadding(dp(18),dp(14),dp(14),dp(14));hero.setBackground(bg(Color.rgb(37,40,67),20));LinearLayout htext=new LinearLayout(this);htext.setOrientation(LinearLayout.VERTICAL);TextView p=tv(profiles[selected],23,TEXT);p.setTypeface(null,Typeface.BOLD);htext.addView(p);htext.addView(tv(profileDesc(selected),12,MUTED));hero.addView(htext,new LinearLayout.LayoutParams(0,dp(72),1));TextView dot=tv("●",27,selected==2?GREEN:ACCENT);hero.addView(dot);content.addView(hero,new LinearLayout.LayoutParams(-1,dp(102)));
        section("QUICK PROFILES");LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<profiles.length;i+=2){LinearLayout r=row();for(int j=i;j<i+2&&j<profiles.length;j++){final int k=j;TextView c=tv((j==0?"⚡  ":j==1?"◉  ":j==2?"☾  ":j==3?"◈  ":"✦  ")+profiles[j],14,j==selected?TEXT:MUTED);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(14),0,dp(8),0);c.setBackground(bg(j==selected?Color.rgb(40,43,67):CARD,14));c.setOnClickListener(v->{selected=k;prefs.edit().putInt("profile",k).apply();showHome();applyProfile(k);});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(54),1);cp.setMargins(0,0,dp(6),dp(6));r.addView(c,cp);}grid.addView(r);}content.addView(grid);
        TextView apply=button("APPLY "+profiles[selected].toUpperCase()+" NOW");apply.setOnClickListener(v->applyProfile(selected));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(50));ap.setMargins(0,dp(6),0,0);content.addView(apply,ap);
        section("DEVICE STATUS");LinearLayout stats=row();LinearLayout.LayoutParams s1=new LinearLayout.LayoutParams(0,dp(92),1),s2=new LinearLayout.LayoutParams(0,dp(92),1);s1.setMargins(0,0,dp(6),0);s2.setMargins(0,0,dp(6),0);stats.addView(stat("CPU",shizukuUp()?"…":"—","max MHz"),s1);stats.addView(stat("RAM","…","used / total"),s2);stats.addView(stat("BATTERY","…","level · temp"),new LinearLayout.LayoutParams(0,dp(92),1));content.addView(stats);
        section("SHIZUKU ACCESS");LinearLayout sh=row();sh.setPadding(dp(14),dp(12),dp(10),dp(12));sh.setBackground(bg(CARD,16));LinearLayout st=new LinearLayout(this);st.setOrientation(LinearLayout.VERTICAL);shizukuStatus=tv("Checking Shizuku…",14,TEXT);st.addView(shizukuStatus);st.addView(tv("ADB-level actions, no root required",11,MUTED));sh.addView(st,new LinearLayout.LayoutParams(0,dp(54),1));TextView act=button("CONNECT");act.setTextSize(12);act.setOnClickListener(v->requestShizuku());sh.addView(act,new LinearLayout.LayoutParams(dp(104),dp(42)));content.addView(sh,new LinearLayout.LayoutParams(-1,dp(82)));updateShizuku();}
    String profileDesc(int i){return i==0?"Animations 0.5×, Doze off — max responsiveness":i==1?"Animations 1.0×, Doze on — factory balance":i==2?"Doze forced immediately — maximum battery saving":i==3?"Animations off, Doze off — pure game speed":("Animations "+customAnim()+"×, your value from Tools");}
    View stat(String name,String value,String sub){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setPadding(dp(12),dp(10),dp(5),dp(8));s.setBackground(bg(CARD,14));s.addView(label(name));TextView v=tv(value,19,TEXT);v.setTypeface(null,Typeface.BOLD);if("CPU".equals(name))cpuView=v;if("RAM".equals(name))ramView=v;if("BATTERY".equals(name))batView=v;s.addView(v);s.addView(tv(sub,10,MUTED));return s;}

    // ---------- Monitor ----------
    void showMonitor(){clear();section("LIVE MONITOR");LinearLayout stats=row();LinearLayout.LayoutParams s1=new LinearLayout.LayoutParams(0,dp(92),1),s2=new LinearLayout.LayoutParams(0,dp(92),1);s1.setMargins(0,0,dp(6),0);s2.setMargins(0,0,dp(6),0);stats.addView(mstat("CPU FREQ","…"),s1);stats.addView(mstat("RAM","…"),s2);stats.addView(mstat("BATTERY","…"),new LinearLayout.LayoutParams(0,dp(92),1));content.addView(stats);
        section("STORAGE & UPTIME");LinearLayout s2r=row();LinearLayout.LayoutParams t1=new LinearLayout.LayoutParams(0,dp(92),1),t2=new LinearLayout.LayoutParams(0,dp(92),1);t1.setMargins(0,0,dp(6),0);s2r.addView(mstat("STORAGE","…"),t1);s2r.addView(mstat("UPTIME","…"),t2);content.addView(s2r);
        section("CAPABILITIES");content.addView(card("✓  Available without root","System info, battery, storage, app usage","Public Android APIs"));
        content.addView(card("◈  Shizuku (ADB level)","Animations, Doze, app freeze, cache trim","settings / dumpsys / pm via Shizuku"));
        content.addView(card("×  Root required","CPU governor, GPU frequency, kernel /sys writes","Not available in no-root mode"));}
    View mstat(String name,String value){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setPadding(dp(12),dp(10),dp(5),dp(8));s.setBackground(bg(CARD,14));s.addView(label(name));TextView v=tv(value,16,TEXT);v.setTypeface(null,Typeface.BOLD);if("CPU FREQ".equals(name))cpuView=v;if("RAM".equals(name))ramView=v;if("BATTERY".equals(name))batView=v;if("STORAGE".equals(name))stoView=v;s.addView(v);return s;}

    // ---------- Tools ----------
    void showTools(){clear();section("ANIMATION SCALE (CUSTOM PROFILE)");LinearLayout a=row();a.setPadding(dp(14),dp(12),dp(14),dp(12));a.setBackground(bg(CARD,16));LinearLayout at=new LinearLayout(this);at.setOrientation(LinearLayout.VERTICAL);final TextView animLabel=tv("Custom profile value: "+customAnim()+"×",14,TEXT);at.addView(animLabel);SeekBar sb=new SeekBar(this);sb.setMax(8);sb.setProgress((int)Math.round(customAnim()/0.25f));sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){prefs.edit().putFloat("customAnim",p*0.25f).apply();animLabel.setText("Custom profile value: "+(p*0.25f)+"×");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});at.addView(sb,new LinearLayout.LayoutParams(-1,dp(40)));a.addView(at,new LinearLayout.LayoutParams(0,dp(86),1));content.addView(a,new LinearLayout.LayoutParams(-1,dp(112)));
        TextView applyC=button("APPLY CUSTOM SCALE NOW");applyC.setOnClickListener(v->{selected=4;prefs.edit().putInt("profile",4).apply();applyProfile(4);});LinearLayout.LayoutParams apc=new LinearLayout.LayoutParams(-1,dp(50));apc.setMargins(0,dp(6),0,0);content.addView(applyC,apc);
        section("SHIZUKU TOOLS");int gap=dp(8);
        TextView doze=button("FORCE DOZE NOW (BATTERY)");doze.setOnClickListener(v->runActions(dozeActs(true),"Force Doze",false));LinearLayout.LayoutParams d1=new LinearLayout.LayoutParams(-1,dp(50));d1.bottomMargin=gap;content.addView(doze,d1);
        TextView undoze=button("UNFORCE DOZE (BACK TO NORMAL)");undoze.setOnClickListener(v->runActions(dozeActs(false),"Unforce Doze",false));LinearLayout.LayoutParams d2=new LinearLayout.LayoutParams(-1,dp(50));d2.bottomMargin=gap;content.addView(undoze,d2);
        TextView freeze=button("FREEZE / UNFREEZE APPS (SUSPEND)");freeze.setOnClickListener(v->showFreezer());LinearLayout.LayoutParams d3=new LinearLayout.LayoutParams(-1,dp(50));d3.bottomMargin=gap;content.addView(freeze,d3);
        TextView trim=button("TRIM ALL APP CACHES");trim.setOnClickListener(v->runActions(Collections.singletonList(new Cmd("pm trim-caches","pm trim-caches 512G",null,null)),"Trim caches",false));LinearLayout.LayoutParams d4=new LinearLayout.LayoutParams(-1,dp(50));d4.bottomMargin=gap;content.addView(trim,d4);
        TextView grant=button("GRANT \"MODIFY SYSTEM SETTINGS\"");grant.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){Toast.makeText(this,"Cannot open settings: "+e.getMessage(),Toast.LENGTH_LONG).show();}});LinearLayout.LayoutParams d5=new LinearLayout.LayoutParams(-1,dp(50));d5.bottomMargin=gap;content.addView(grant,d5);
        section("GAME MODE AUTOMATION");LinearLayout gm=row();gm.setPadding(dp(14),dp(12),dp(14),dp(12));gm.setBackground(bg(CARD,16));LinearLayout gml=new LinearLayout(this);gml.setOrientation(LinearLayout.VERTICAL);gml.addView(tv("Auto Game Mode: "+(prefs.getBoolean("gameAuto",false)?"ON":"OFF"),14,prefs.getBoolean("gameAuto",false)?GREEN:TEXT));gml.addView(tv("Applies Game profile when a game opens, restores after. Needs Usage Access.",11,MUTED));gm.addView(gml,new LinearLayout.LayoutParams(0,dp(54),1));Switch sw=new Switch(this);sw.setChecked(prefs.getBoolean("gameAuto",false));sw.setOnCheckedChangeListener((b,c)->{prefs.edit().putBoolean("gameAuto",c).apply();gameAutoActive=false;if(c){if(!hasUsageAccess())Toast.makeText(this,"Grant Usage Access on the next screen",Toast.LENGTH_LONG).show();try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}catch(Exception ignored){}}showTools();});gm.addView(sw);content.addView(gm,new LinearLayout.LayoutParams(-1,dp(86)));
        section("BACKUP");TextView restore=button("RESTORE SNAPSHOT (UNDO LAST APPLY)");restore.setOnClickListener(v->restoreSnapshot(false));content.addView(restore,new LinearLayout.LayoutParams(-1,dp(50)));
        section("IMPORTANT");content.addView(card("NO FAKE SUCCESS","Every action shows the real command result","Failures are reported, never hidden"));content.addView(card("NO ROOT GUARANTEE","Unsupported kernel controls are never faked or written","This app will not damage your device"));}

    void about(){AlertDialog.Builder b=new AlertDialog.Builder(this);b.setTitle("DraB Tweak");String ver;try{ver=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){ver="?";}
        b.setMessage("Version "+ver+"\n\nShizuku: "+(shizukuUp()?"running":"not running")+"\nModify system settings: "+(Settings.System.canWrite(this)?"granted":"not granted")+"\nUsage Access: "+(hasUsageAccess()?"granted":"not granted"));b.setPositiveButton("OK",null);b.show();}

    // ---------- profile engine: REAL apply + honest verification ----------
    float customAnim(){return prefs.getFloat("customAnim",0.75f);}
    class Cmd{String label,cmd,verifyKey,expect;Cmd(String l,String c,String vk,String e){label=l;cmd=c;verifyKey=vk;expect=e;}}
    List<Cmd> animActs(float anim){List<Cmd> a=new ArrayList<>();for(String k:new String[]{"window_animation_scale","transition_animation_scale","animator_duration_scale"})a.add(new Cmd(k,"settings put global "+k+" "+anim,k,anim+""));return a;}
    List<Cmd> dozeActs(boolean force){List<Cmd> a=new ArrayList<>();a.add(new Cmd(force?"dumpsys deviceidle force-idle":"dumpsys deviceidle unforce",force?"dumpsys deviceidle force-idle":"dumpsys deviceidle unforce",null,null));return a;}
    boolean shizukuUp(){try{return Shizuku.pingBinder();}catch(Throwable t){return false;}}
    boolean shizukuReady(){try{return Shizuku.pingBinder()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(Throwable t){return false;}}

    void applyProfile(int idx){if(!applying.compareAndSet(false,true)){Toast.makeText(this,"Another action is running",Toast.LENGTH_SHORT).show();return;}
        float anim=idx==0?0.5f:idx==1?1.0f:idx==2?1.0f:idx==3?0.0f:customAnim();boolean dozeOff=idx==0||idx==3;
        snapshot();List<Cmd> acts=new ArrayList<>(animActs(anim));
        if(dozeOff){acts.add(new Cmd("dumpsys deviceidle disable","dumpsys deviceidle disable",null,null));}
        else if(idx==2){acts.add(new Cmd("dumpsys deviceidle enable","dumpsys deviceidle enable",null,null));acts.addAll(dozeActs(true));}
        runActions(acts,"Profile: "+profiles[idx],false);}

    void runActions(final List<Cmd> acts,final String title,final boolean quiet){final boolean shz=shizukuReady();final boolean canWrite=Settings.System.canWrite(this);
        if(!shz&&!canWrite){applying.set(false);new AlertDialog.Builder(this).setTitle(title+" — not applied").setMessage("No execution path available.\n\n• Connect Shizuku (ADB level) for all actions\n• Or grant \"Modify system settings\" for animation-only actions").setPositiveButton("Open Shizuku site",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/")))).setNeutralButton("Grant write settings",(d,w)->{try{startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}).setNegativeButton("Cancel",null).show();return;}
        new Thread(()->{final StringBuilder ok=new StringBuilder(),fail=new StringBuilder();
            for(Cmd c:acts){String line;
                if(shz){String[] r=sh(c.cmd);String rb=c.verifyKey!=null?sh("settings get global "+c.verifyKey)[1]:null;boolean good="0".equals(r[0])&&(c.verifyKey==null||close(rb,c.expect));
                    String detail=c.verifyKey!=null?(" = "+trim(rb)):(!trim(r[1]).isEmpty()?" → "+trim(r[1]):"");
                    line=(good?"OK  ":"FAIL ")+c.label+detail;(good?ok:fail).append(line).append("\n");}
                else if(canWrite&&c.verifyKey!=null){try{Settings.Global.putFloat(getContentResolver(),c.verifyKey,Float.parseFloat(c.expect));line="OK  "+c.label+" = "+c.expect;ok.append(line).append("\n");}catch(Exception e){line="FAIL "+c.label+" → "+e.getMessage();fail.append(line).append("\n");}}
                else{line="SKIP "+c.label+" (needs Shizuku)";fail.append(line).append("\n");}}
            final String report=(ok.length()>0?"APPLIED:\n"+ok:"")+(fail.length()>0?"\nPROBLEMS:\n"+fail:"");
            handler.post(()->{applying.set(false);updateHomeStatus();
                if(quiet){if(fail.length()>0)Toast.makeText(MainActivity.this,title+": "+countLines(fail)+" action(s) failed",Toast.LENGTH_LONG).show();}
                else new AlertDialog.Builder(MainActivity.this).setTitle(title+" — result").setMessage(report.trim().isEmpty()?"No actions":report.trim()).setPositiveButton("OK",null).show();});}).start();}
    int countLines(StringBuilder s){int n=0;for(char c:s.toString().toCharArray())if(c=='\n')n++;return n;}

    boolean close(String actual,String expect){try{return Math.abs(Float.parseFloat(trim(actual))-Float.parseFloat(expect))<0.01f;}catch(Exception e){return false;}}
    String trim(String s){return s==null?"":s.trim().replace("\n"," ");}

    /** Runs one shell command through the real Shizuku bridge (UserService, shell uid). Returns {exitCode, combinedOutput}. */
    String[] sh(String cmd){IShellService s=shell;if(s==null){if(shizukuReady())bindShell();return new String[]{"-1","Shizuku service not connected"};}try{return s.exec(cmd);}catch(Throwable t){return new String[]{"-1",t.getMessage()==null?t.getClass().getSimpleName():t.getMessage()};}}
    void bindShell(){if(shellBound||!shizukuReady())return;try{Shizuku.bindUserService(shellArgs,shellConn);shellBound=true;}catch(Throwable ignored){}}

    void snapshot(){SharedPreferences.Editor e=prefs.edit();e.putFloat("s_window",Settings.Global.getFloat(getContentResolver(),"window_animation_scale",1f));e.putFloat("s_transition",Settings.Global.getFloat(getContentResolver(),"transition_animation_scale",1f));e.putFloat("s_animator",Settings.Global.getFloat(getContentResolver(),"animator_duration_scale",1f));e.putBoolean("has_snapshot",true);e.apply();}
    void restoreSnapshot(boolean quiet){if(!prefs.getBoolean("has_snapshot",false)){if(!quiet)Toast.makeText(this,"No snapshot yet — apply a profile first",Toast.LENGTH_LONG).show();applying.set(false);return;}
        List<Cmd> a=new ArrayList<>();float w=prefs.getFloat("s_window",1f),t=prefs.getFloat("s_transition",1f),n=prefs.getFloat("s_animator",1f);
        a.add(new Cmd("window_animation_scale","settings put global window_animation_scale "+w,"window_animation_scale",w+""));
        a.add(new Cmd("transition_animation_scale","settings put global transition_animation_scale "+t,"transition_animation_scale",t+""));
        a.add(new Cmd("animator_duration_scale","settings put global animator_duration_scale "+n,"animator_duration_scale",n+""));
        a.add(new Cmd("dumpsys deviceidle unforce","dumpsys deviceidle unforce",null,null));
        runActions(a,"Restore snapshot",quiet);}
    void updateHomeStatus(){try{if(shizukuStatus!=null)updateShizuku();}catch(Throwable ignored){}}

    // ---------- freezer ----------
    void showFreezer(){if(!shizukuReady()){Toast.makeText(this,"Connect Shizuku first (Freeze needs ADB level)",Toast.LENGTH_LONG).show();requestShizuku();return;}
        final List<ApplicationInfo> user=new ArrayList<>();for(ApplicationInfo ai:getPackageManager().getInstalledApplications(0))if((ai.flags&ApplicationInfo.FLAG_SYSTEM)==0)user.add(ai);Collections.sort(user,(x,y)->x.packageName.compareTo(y.packageName));
        Set<String> suspended=prefs.getStringSet("suspended",new HashSet<>());
        final String[] names=new String[user.size()];final boolean[] st=new boolean[user.size()];
        for(int i=0;i<user.size();i++){names[i]=user.get(i).packageName;st[i]=suspended.contains(names[i]);}
        new AlertDialog.Builder(this).setTitle("Freeze / unfreeze apps").setMultiChoiceItems(names,st,(d,which,on)->{}).setPositiveButton("Apply",(d,w)->{if(!applying.compareAndSet(false,true)){Toast.makeText(this,"Another action is running",Toast.LENGTH_SHORT).show();return;}new Thread(()->{int okc=0,failc=0,skip=0;StringBuilder fails=new StringBuilder();Set<String> now=new HashSet<>();
            for(int i=0;i<user.size();i++){String pk=names[i];boolean want=st[i];boolean was=suspended.contains(pk);if(want==was){if(want)now.add(pk);skip++;continue;}String[] r=sh((want?"pm suspend --user 0 ":"pm unsuspend --user 0 ")+pk);boolean good="0".equals(r[0]);if(good){if(want)now.add(pk);okc++;}else{failc++;fails.append(pk).append(": ").append(trim(r[1])).append("\n");}}
            prefs.edit().putStringSet("suspended",now).apply();
            final String rep=okc+" applied, "+skip+" unchanged, "+failc+" failed"+(failc>0?"\n\n"+fails:"");
            handler.post(()->{applying.set(false);new AlertDialog.Builder(MainActivity.this).setTitle("Freezer result").setMessage(rep).setPositiveButton("OK",null).show();});}).start();}).setNegativeButton("Cancel",null).show();}

    // ---------- Shizuku ----------
    void updateShizuku(){try{boolean up=Shizuku.pingBinder();boolean ok=up&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;shizukuStatus.setText(ok?"Shizuku is ready":up?"Shizuku running — permission needed":"Shizuku is not running");shizukuStatus.setTextColor(ok?GREEN:ORANGE);if(ok)bindShell();}catch(Throwable e){shizukuStatus.setText("Install Shizuku to unlock tools");shizukuStatus.setTextColor(ORANGE);}}
    void requestShizuku(){try{if(!Shizuku.pingBinder()){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/")));return;}if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)Shizuku.requestPermission(100);updateShizuku();}catch(Throwable e){Toast.makeText(this,"Install and start Shizuku first",Toast.LENGTH_LONG).show();}}

    // ---------- usage access / game automation ----------
    boolean hasUsageAccess(){try{AppOpsManager a=(AppOpsManager)getSystemService(APP_OPS_SERVICE);int m=a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName());return m==AppOpsManager.MODE_ALLOWED;}catch(Throwable t){return false;}}
    String foregroundGame(){try{UsageStatsManager u=(UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);UsageEvents ev=u.queryEvents(System.currentTimeMillis()-15000,System.currentTimeMillis());UsageEvents.Event e=new UsageEvents.Event();String fg=null;while(ev.hasNextEvent()){ev.getNextEvent(e);if(e.getEventType()==UsageEvents.Event.MOVE_TO_FOREGROUND)fg=e.getPackageName();}if(fg==null)return null;ApplicationInfo ai=getPackageManager().getApplicationInfo(fg,0);return((ai.flags&ApplicationInfo.FLAG_IS_GAME)!=0||(Build.VERSION.SDK_INT>=26&&ai.category==ApplicationInfo.CATEGORY_GAME))?fg:null;}catch(Throwable t){return null;}}
    void checkGameAuto(){if(!prefs.getBoolean("gameAuto",false)||!hasUsageAccess())return;if(!applying.compareAndSet(false,true))return;String g=foregroundGame();
        if(g!=null&&!gameAutoActive){gameAutoActive=true;selected=3;prefs.edit().putInt("profile",3).apply();snapshot();List<Cmd> acts=new ArrayList<>(animActs(0f));acts.add(new Cmd("dumpsys deviceidle disable","dumpsys deviceidle disable",null,null));runActions(acts,"Auto Game Mode",true);}
        else if(g==null&&gameAutoActive){gameAutoActive=false;restoreSnapshot(true);}
        else applying.set(false);}

    // ---------- live tick ----------
    Runnable tick=new Runnable(){int n=0;public void run(){n++;
        try{ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();am.getMemoryInfo(m);BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);int bat=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            Intent bi=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));float temp=bi==null?0f:bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f;
            if(ramView!=null)ramView.setText((m.totalMem-m.availMem)/1048576+"/"+m.totalMem/1048576+" MB");
            if(batView!=null)batView.setText(bat+"% · "+temp+"°C");
            if(stoView!=null&&n%5==0){StatFs fs=new StatFs(Environment.getDataDirectory().getPath());stoView.setText((fs.getTotalBytes()-fs.getAvailableBytes())/1073741824+"/"+fs.getTotalBytes()/1073741824+" GB");}
            if(cpuView!=null&&n%3==0&&cpuBusy.compareAndSet(false,true)){if(!shizukuUp()){cpuView.setText("—");cpuBusy.set(false);}else new Thread(()->{String[] r=sh("for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do cat $f 2>/dev/null; done | sort -n | tail -1");int mhz=0;try{mhz=Integer.parseInt(trim(r[1]).replaceAll("\\D",""))/1000;}catch(Exception ignored){}final int fm=mhz;handler.post(()->{if(cpuView!=null)cpuView.setText(fm>0?fm+" MHz":"—");cpuBusy.set(false);});}).start();}
            if(n%3==0)checkGameAuto();
        }catch(Throwable ignored){}
        handler.postDelayed(this,1000);}};
}
