package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class WeaponActionBarTest {
    @Test void onlyCooldownAndChargeStatesAreShownInActionBar() throws Exception {
        var root=Path.of("src/main/java/dev/emerald/funnel");
        var pattern=Pattern.compile("sendActionBar\\(Component\\.text\\(([\\s\\S]*?)\\)\\);");
        int count=0;
        try(var files=Files.walk(root)){
            for(var file:files.filter(p->p.toString().endsWith(".java")).toList()){
                var matcher=pattern.matcher(Files.readString(file));
                while(matcher.find()){
                    String message=matcher.group(1);
                    assertTrue(message.contains("冷却中")||message.contains("クールダウン")
                        ||message.contains("チャージ")||message.contains("装填")||message.contains("充填"),
                        file+": "+message);
                    assertFalse(message.contains("発射！")||message.contains("展開")||message.contains("収納")
                        ||message.contains("連射中")||message.contains("射撃終了"),file+": "+message);
                    count++;
                }
            }
        }
        assertTrue(count>=10,"Cooldown and charge notices must remain");
    }
}
