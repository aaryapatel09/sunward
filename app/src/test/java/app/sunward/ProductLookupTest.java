package app.sunward;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductLookupTest {
    @Test public void validGtinAndChecksum(){
        assertTrue(ProductRules.validCode("96385074"));
        assertTrue(ProductRules.validCode("042100005264"));
        assertTrue(ProductRules.validCode("3337875586115"));
        assertTrue(ProductRules.validCode("03337875586115"));
        assertFalse(ProductRules.validCode("3337875586114"));
        assertFalse(ProductRules.validCode("00000000"));
        assertFalse(ProductRules.validCode("https://example.com"));
        assertFalse(ProductRules.validCode("3337875586115/evil"));
        assertEquals("3337875586115",ProductRules.cleanCode(" 3337-875586115 "));
    }
    @Test public void upceExpansion(){
        assertEquals("042100005264",ProductRules.expandUpce("04252614"));
        assertTrue(ProductRules.validCode(ProductRules.expandUpce("04252614")));
        assertEquals("",ProductRules.expandUpce("bad"));
    }
    @Test public void leadingZeroEquivalence(){
        assertTrue(ProductRules.sameCode("042100005264","0042100005264"));
        assertFalse(ProductRules.sameCode("042100005264","3337875586115"));
    }
    @Test public void suggestOnlyExplicitSpf(){
        assertEquals("50",ProductRules.suggestedSpf("Anthelios SPF 50+ 200 ml"));
        assertEquals("30",ProductRules.suggestedSpf("spf: 30"));
        assertEquals("",ProductRules.suggestedSpf("Sunscreen 50 ml"));
        assertEquals("",ProductRules.suggestedSpf("SPF 30 / SPF 50"));
        assertEquals("",ProductRules.suggestedSpf("SPF 5000"));
        assertEquals("",ProductRules.suggestedSpf("SPF 0"));
        assertEquals("",ProductRules.suggestedSpf("SPF 30/50"));
    }
    @Test public void foundMatch() throws Exception {
        ProductLookup.Product p=ProductLookup.parse("{\"status\":1,\"product\":{\"code\":\"3337875586115\",\"product_name\":\"Anthelios SPF 50+\",\"brands\":\"La Roche-Posay\"}}","3337875586115");
        assertEquals("La Roche-Posay · Anthelios SPF 50+",p.displayName());assertEquals("50",p.spf);
    }
    @Test public void fallbackNameDoesNotInventSpf() throws Exception {
        ProductLookup.Product p=ProductLookup.parse("{\"status\":1,\"code\":\"3337875586115\",\"product\":{\"product_name\":null,\"product_name_en\":\"Sunscreen\",\"brands\":null}}","3337875586115");
        assertEquals("Sunscreen",p.displayName());assertEquals("",p.spf);
    }
    @Test public void rejectMissingWrongAndMalformedResponses(){
        rejected("{\"status\":0}","No match");
        rejected("{\"status\":1}","incomplete");
        rejected("{\"status\":1,\"product\":{\"code\":\"042100005264\",\"product_name\":\"Wrong product\"}}","does not match");
        rejected("{\"status\":1,\"product\":{\"code\":\"3337875586115\"}}","no product name");
        rejected("<html>Service unavailable</html>","unreadable");
    }
    private void rejected(String json,String expected){try{ProductLookup.parse(json,"3337875586115");fail("Should reject");}catch(ProductLookup.LookupException e){assertTrue(e.getMessage(),e.getMessage().contains(expected));}}
    @Test public void preserveTimerInvariant(){
        long applied=1000000L;
        for(int water:new int[]{0,40,80})for(int elapsed=0;elapsed<180;elapsed++)assertTrue(Rules.due(applied,applied+elapsed*Rules.MINUTE,water)<=applied+120*Rules.MINUTE);
    }
}
