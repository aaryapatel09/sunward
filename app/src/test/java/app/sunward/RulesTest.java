package app.sunward;
public class RulesTest {
    static int checks;
    static void eq(long a,long b){checks++;if(a!=b)throw new AssertionError(a+" != "+b);}
    static void eq(String a,String b){checks++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    @org.junit.Test public void existingTimerAndUvChecks(){
        long applied=1_000_000L, m=Rules.MINUTE;
        eq(Rules.due(applied,0,0),applied+120*m);
        eq(Rules.due(applied,0,80),applied+120*m);
        eq(Rules.due(applied,applied,40),applied+40*m);
        eq(Rules.due(applied,applied,80),applied+80*m);
        eq(Rules.due(applied,applied+100*m,80),applied+120*m);
        eq(Rules.due(applied,applied+10*m,0),applied+10*m);
        eq(Rules.due(applied,applied+10*m,99),applied+10*m);
        eq(Rules.uvBand(Double.NaN),"Unavailable");eq(Rules.uvBand(-1),"Unavailable");
        eq(Rules.uvBand(2.9),"Low");eq(Rules.uvBand(3),"Moderate");eq(Rules.uvBand(6),"High");eq(Rules.uvBand(8),"Very high");eq(Rules.uvBand(11),"Extreme");
        eq(Rules.nearestHour(new long[]{10000,13600,17200},14000),1);
        eq(Rules.nearestHour(new long[]{10000,13600,17200},25000),-1);
        eq(Rules.nearestHour(new long[]{},10000),-1);
        for(int water:new int[]{0,40,80})for(int elapsed=0;elapsed<180;elapsed++){
            long due=Rules.due(applied,applied+elapsed*m,water);
            checks++;if(due>applied+120*m)throw new AssertionError("Wet activity extended baseline");
        }
        System.out.println(checks+" checks passed");
    }
}
