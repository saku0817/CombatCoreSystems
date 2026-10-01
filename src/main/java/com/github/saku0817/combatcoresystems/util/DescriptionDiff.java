package com.github.saku0817.combatcoresystems.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.*;

/** Unicode-aware linear-space LCS. Only inserted/replaced visible text is recolored. */
public final class DescriptionDiff {
    private DescriptionDiff() {}
    private record Pair(String before,String after) {}
    private static final Map<Pair,boolean[]> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Pair,boolean[]> entry) { return size()>128; }
    });
    private static final PlainTextComponentSerializer PLAIN=PlainTextComponentSerializer.plainText();
    public static List<Component> highlight(List<Component> before,List<Component> after) {
        String a=String.join("\n",before.stream().map(PLAIN::serialize).toList());
        String b=String.join("\n",after.stream().map(PLAIN::serialize).toList());
        if(a.equals(b))return List.copyOf(after);
        boolean[] unchanged=CACHE.computeIfAbsent(new Pair(a,b),pair->unchanged(pair.before(),pair.after()));
        int[] cursor={0};List<Component> result=new ArrayList<>();
        for(Component line:after) {result.add(recolor(line,unchanged,cursor));cursor[0]++;}
        return List.copyOf(result);
    }
    private static Component recolor(Component node,boolean[] unchanged,int[] cursor) {
        List<Component> children=node.children();Component output;
        if(node instanceof TextComponent text) {
            output=Component.empty().style(node.style());
            StringBuilder fragment=new StringBuilder();Boolean same=null;
            for(int cp:text.content().codePoints().toArray()) {
                boolean keep=cursor[0]<unchanged.length&&unchanged[cursor[0]++];
                if(same!=null&&same!=keep) {output=output.append(piece(fragment.toString(),same));fragment.setLength(0);}
                fragment.appendCodePoint(cp);same=keep;
            }
            if(same!=null)output=output.append(piece(fragment.toString(),same));
        } else {
            output=node.children(List.of());
            cursor[0]+=PLAIN.serialize(output).codePointCount(0,PLAIN.serialize(output).length());
        }
        for(Component child:children)output=output.append(recolor(child,unchanged,cursor));
        return output;
    }
    private static Component piece(String text,boolean unchanged) {
        return unchanged?Component.text(text):Component.text(text).color(NamedTextColor.YELLOW);
    }
    static boolean[] unchanged(String before,String after) {
        int[] a=before.codePoints().toArray(),b=after.codePoints().toArray();boolean[] result=new boolean[b.length];
        match(a,0,a.length,b,0,b.length,result);return result;
    }
    private static void match(int[] a,int lo,int hi,int[] b,int start,int end,boolean[] result) {
        while(lo<hi&&start<end&&a[lo]==b[start]) {result[start++]=true;lo++;}
        while(lo<hi&&start<end&&a[hi-1]==b[end-1]) {result[--end]=true;hi--;}
        if(lo==hi||start==end)return;
        if(hi-lo==1) {for(int j=start;j<end;j++)if(a[lo]==b[j]){result[j]=true;break;}return;}
        int middle=(lo+hi)/2;
        int[] left=row(a,lo,middle,b,start,end,false),right=row(a,middle,hi,b,start,end,true);
        int split=0,best=-1,length=end-start;
        for(int j=0;j<=length;j++)if(left[j]+right[length-j]>best){best=left[j]+right[length-j];split=j;}
        match(a,lo,middle,b,start,start+split,result);match(a,middle,hi,b,start+split,end,result);
    }
    private static int[] row(int[] a,int lo,int hi,int[] b,int start,int end,boolean reverse) {
        int[] previous=new int[end-start+1],next=new int[end-start+1];
        for(int i=0;i<hi-lo;i++) {
            next[0]=0;int cp=a[reverse?hi-1-i:lo+i];
            for(int j=1;j<=end-start;j++)next[j]=cp==b[reverse?end-j:start+j-1]?previous[j-1]+1:Math.max(previous[j],next[j-1]);
            int[] temp=previous;previous=next;next=temp;
        }
        return previous;
    }
}
