package com.github.saku0817.combatcoresystems.util;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DescriptionDiffTest {
    @Test void onlyChangedDigitsTurnYellowAndOriginalStylesRemain() {
        var current=ItemText.parse("<red><bold>攻撃力50%、10秒</bold></red>");
        var result=DescriptionDiff.highlight(List.of(ItemText.parse("攻撃力30%、10秒")),List.of(current)).getFirst();
        assertEquals("攻撃力50%、10秒",PlainTextComponentSerializer.plainText().serialize(result));
        List<TextColor> colors=new ArrayList<>();colors(result,NamedTextColor.WHITE,colors);
        assertEquals(NamedTextColor.YELLOW,colors.get(3));
        assertEquals(NamedTextColor.RED,colors.get(4));
        assertEquals(1,colors.stream().filter(NamedTextColor.YELLOW::equals).count());
        assertTrue(hasBold(result));
    }
    @Test void insertedLineDoesNotRecolorTheUnchangedFollowingLine() {
        var old=List.of(ItemText.parse("前"),ItemText.parse("後"));
        var after=List.of(ItemText.parse("前"),ItemText.parse("追加"),ItemText.parse("後"));
        var result=DescriptionDiff.highlight(old,after);
        List<TextColor> colors=new ArrayList<>();colors(result.get(2),NamedTextColor.WHITE,colors);
        assertEquals(List.of(NamedTextColor.WHITE),colors);
    }
    @Test void emojiAndDeletionAreHandledWithoutBrokenSurrogates() {
        assertArrayEquals(new boolean[]{true,false,true},DescriptionDiff.unchanged("星🌞光","星🌙光"));
        assertArrayEquals(new boolean[]{true,true},DescriptionDiff.unchanged("ABC","AC"));
        assertArrayEquals(new boolean[]{true,true,false,true,true},DescriptionDiff.unchanged("abcd","abXcd"));
    }
    private void colors(Component node,TextColor inherited,List<TextColor> out) {
        TextColor color=node.color()==null?inherited:node.color();
        if(node instanceof TextComponent text)text.content().codePoints().forEach(cp->out.add(color));
        node.children().forEach(child->colors(child,color,out));
    }
    private boolean hasBold(Component c) {return c.decoration(TextDecoration.BOLD)==TextDecoration.State.TRUE||c.children().stream().anyMatch(this::hasBold);}
}
