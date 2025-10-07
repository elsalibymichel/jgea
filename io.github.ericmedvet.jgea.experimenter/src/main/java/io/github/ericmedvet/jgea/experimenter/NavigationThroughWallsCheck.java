package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.InvertibleMapper;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;

import java.io.File;
import java.util.List;
import java.util.function.Function;

public class NavigationThroughWallsCheck {
  private final static String MAPPER_S = """
      ea.m.enhancedNds(
        of = ea.m.dsToNpnds(npnds = ds.num.mlp(innerLayers = [8]));
        windowT = 0.5
      )
      """;
  private final static String SIM_S = """
      ds.sat.fromEnvironment(
        dT = 0.1;
        tRange = m.range(min = 0; max = 60);
        environment = ds.e.navigation(
          nOfSensors = 7;
          robotRadius = 0.02;
          arena = ds.arena.fromString(
            name = "s4";
            l = 0.25;
            emptyChar = e;
            s = "eeweeweeee|eewwwwwtee|eewewwweee|eeweweweee|eewewwwewe|eeweweweww|eewwwwweee|eeeeeeweee|sewwwwweee|eeeeeewewe"
          )
        )
      )
      """;
  private final static String GENO_S =
      "rO0ABXNyABFqYXZhLnV0aWwuQ29sbFNlcleOq7Y6G6gRAwABSQADdGFneHAAAAAEdwQAAAABc3EAfgAAAAAABHcEAAABCnNyABBqYXZhLmxhbmcuRG91YmxlgLPCSilr+wQCAAFEAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cL/7IDw8jhtec3EAfgADwADER5/Hn6xzcQB+AAO/5IC8GS/4N3NxAH4AA0ABV0F8YusQc3EAfgADP/omROg9fQNzcQB+AAM/5BM1xgky6nNxAH4AA7/xNUIodC3Oc3EAfgADv94j39dL+ERzcQB+AAO/+7+ts3eo7XNxAH4AAz+uR4yrk2GAc3EAfgADP/gIsM1OncBzcQB+AAO//As4l3JlbXNxAH4AA0AEUxpv/2UNc3EAfgADwAKNzjUrDd1zcQB+AAO/7zUFbsmVfXNxAH4AAz/aUgvbQH+ac3EAfgADP7gynNtz7flzcQB+AAO/6FjKCzdF9XNxAH4AA7/YID2ToPHdc3EAfgADwAD1R/sZVgFzcQB+AAO//O53v85d1XNxAH4AA7/0D52aAL8Rc3EAfgADwAWTeKpg0O9zcQB+AAO/+fniMLzu5nNxAH4AAz/31aa2A8acc3EAfgADwAlvVXtDwsBzcQB+AAO/fJq9sfeDIHNxAH4AA0AGESvTH66Oc3EAfgADv+hWcy/YlMVzcQB+AAO/5sU4iOATe3NxAH4AA7+t3iEdCWfoc3EAfgADv7O4pe+rJYhzcQB+AAM/71pkGCFDNHNxAH4AA7/uoeB7Gco3c3EAfgADv+UirdMPSjpzcQB+AAPAAv/gIxScL3NxAH4AA7/qK7d8BbL+c3EAfgADv/aOlJ9YtPtzcQB+AAM/8tzC5RHYPXNxAH4AA7/yArXYkiKRc3EAfgADv9mW/RwmwAtzcQB+AAO/5zjf/tsA5nNxAH4AA7/8c2iM3fHbc3EAfgADP9RAE99vLE9zcQB+AAPAAT/sqHSl2XNxAH4AAz/3K3glJO5kc3EAfgADP9wnEF5XZF5zcQB+AAM/0E1GwhB3uXNxAH4AA8ACDDMtBR1Vc3EAfgADP/eEBcCyvEJzcQB+AAO/5GYKqiPb63NxAH4AAz/0DjUnPe5mc3EAfgADP86Wn6hLc3hzcQB+AAO/+WnHs5gF9HNxAH4AA7/1WV9mQ22Oc3EAfgADP/AnghgtOfJzcQB+AAM/1UiF+sPilXNxAH4AA7/tll/4y3OTc3EAfgADP+B/ZjUE1RhzcQB+AAM/5dtkpDxe6XNxAH4AA7/vMzdIuuEPc3EAfgADP/TX7CYb/mxzcQB+AAO/6qibsK9/O3NxAH4AAz+z4lxtz39ac3EAfgADv9A0y4pt9R5zcQB+AAO/+CAP/0VVnnNxAH4AA7/ypwfD7HWZc3EAfgADv9Xd88Wl3aFzcQB+AAO/2eFSvN8a03NxAH4AA0AA8pg8Qhxtc3EAfgADv+bXSQQzgx5zcQB+AAM/8DhvWu0973NxAH4AA0ALzl/xnJOxc3EAfgADP+Y7sLiw1D9zcQB+AAO/6Hg3OliQ4XNxAH4AA7/tD5Fwbs6kc3EAfgADP8xLFZpv1tpzcQB+AAPAADTJU39LeHNxAH4AAz/mXiB3anqIc3EAfgADv9pkXbl3mC1zcQB+AAO/+Eho7pUsynNxAH4AA0AIgnWMkFOKc3EAfgADv/QKrLbtYkBzcQB+AAPACqq/ipo+0HNxAH4AA7/9kB/CuFntc3EAfgADv/DHCDNFibZzcQB+AAO/8/BQi8D6s3NxAH4AAz/o3LQyFWfAc3EAfgADQAR9qwEwiedzcQB+AAM/+CLNSX9sHHNxAH4AA7/16J0zESkuc3EAfgADP8anWrYW7x5zcQB+AAM/5e8L9/I+U3NxAH4AA0ACkJp1jOqcc3EAfgADP+OZSfv387pzcQB+AAM/2o30W7QeKXNxAH4AA8AEib+5dY64c3EAfgADv/2KJvLzrzJzcQB+AAPAAET02OrYAHNxAH4AA7/VlFNiDpzCc3EAfgADwA+mn7lYhtFzcQB+AAPACRtMkAVwAnNxAH4AAz/n1kb4UrWWc3EAfgADP+ENm5WDcFBzcQB+AAM/tYbhgYvtanNxAH4AA7/2TT3LdLblc3EAfgADwAnJK1M/EyNzcQB+AAO/+5gSY1ZZF3NxAH4AAz/6HQ4A5Mdlc3EAfgADv7YczE0ekkBzcQB+AANAAYtTBisCAHNxAH4AAz/128Trqseac3EAfgADQAA+yfSf6ANzcQB+AAPABBSZdzw+YHNxAH4AA7/xC78Zgurkc3EAfgADv+hPootM+qlzcQB+AAM/233UjnpdYXNxAH4AAz/dMXMlbMXHc3EAfgADv+t0NVv8eqhzcQB+AAM/TCViJkJ8AHNxAH4AAz/3tBurZ/XGc3EAfgADP874Ji9IMLxzcQB+AAO/v8GmY+o72HNxAH4AA8ALLgu6js9Rc3EAfgADP+9zomG1t49zcQB+AAPABIexsS+8NHNxAH4AA8AGET5ysTNKc3EAfgADv/wi8uGdPBhzcQB+AAO/9jnHBVt/I3NxAH4AAz/hN8hvVxyEc3EAfgADv/i5xjwJXZdzcQB+AAM/+plUknkpZXNxAH4AAz/7FIYKlyMTc3EAfgADwAoyQwoIUupzcQB+AAO/3RzjsI5y/XNxAH4AA7/jFxTbUWZac3EAfgADv7vpmwsyfg5zcQB+AANABU37f220mnNxAH4AA7/haEc+UGQ6c3EAfgADP/23eRUagCdzcQB+AAO/4ZnlfQYRUnNxAH4AAz+zPh6Uq5D1c3EAfgADv+ayMH0dC4pzcQB+AAM/58Xw0Ma2o3NxAH4AA0AAub3UL9m8c3EAfgADv/SEjMj4HtVzcQB+AAO/5tZRP8tzSHNxAH4AA7/9KOnL8NVWc3EAfgADP/lJdwBV9o1zcQB+AAPACe2MQ9PNRHNxAH4AAz/tQmiyy5qWc3EAfgADv+1zUFQyA15zcQB+AAM//33f3ZP56HNxAH4AAz/8fa7PlQmQc3EAfgADv87RwPyrAotzcQB+AAM/fibQKdJxQHNxAH4AA7+O3yjuwnjgc3EAfgADv/a6FWMUh11zcQB+AAO/+XjtMvXMRHNxAH4AA8AQjnkZx3vWc3EAfgADv/O1xhMA1TFzcQB+AAM/4/slYu3v33NxAH4AA7++uIoOFz2uc3EAfgADQA8mBvyLjjJzcQB+AAO/8EsOvi5ofXNxAH4AAz/sRkqvfNJZc3EAfgADv9vRhJeGy9BzcQB+AAO/6CK7jnfPXHNxAH4AA0ACYerIMMlKc3EAfgADP+i07TGa51ZzcQB+AAPACuwk7z1BnXNxAH4AA0ANMSO53jboc3EAfgADv/CGAY02mUFzcQB+AAM/7E6hBlDcC3NxAH4AA7/1x+M3Cnztc3EAfgADv+4m1mN6a3pzcQB+AAM/8+CvBGwNuHNxAH4AA7//EUDvtDVsc3EAfgADv/pznpCtvdpzcQB+AAM/81clbqg3xnNxAH4AA0AD1AZxmM/tc3EAfgADP7FDXLVQXgBzcQB+AAO//ral9ySW7nNxAH4AAz/2upQ/rrokc3EAfgADv+Xq9HzOh79zcQB+AAM/7/yq/oz6rXNxAH4AA7/weh0NUvVoc3EAfgADP+kvljMBzMBzcQB+AAPABhTMiTx1dnNxAH4AAz/+cHp4jNW6c3EAfgADP8agR6yWb/5zcQB+AAO//vflpsdWCnNxAH4AAz/wYWZmMREsc3EAfgADP+SGbIQbA61zcQB+AAPADIjgcwM5RnNxAH4AAz/21GJm6J8Ac3EAfgADv+AnIkGdcJlzcQB+AAPABflErjNUcHNxAH4AA8ADffjqCykPc3EAfgADP9qNJFpxsGpzcQB+AAM/7dD/FiW8AXNxAH4AA7/0POtdJdCcc3EAfgADv8g8Yj1L/6xzcQB+AAO/xfotMwJeVHNxAH4AA7/Ud6YX8IFxc3EAfgADv82wz0eu6vpzcQB+AAM/5KfxqsAcPnNxAH4AAz/8wRI1A+whc3EAfgADv9Hj7hGn7d5zcQB+AAPAC5A1klRAu3NxAH4AA7+yKBOBKmRgc3EAfgADP/gxAga0xmRzcQB+AAM/+0Emnao7D3NxAH4AA0AOxPuoRt1gc3EAfgADv+peNZphcT1zcQB+AAM/6b3jjc2pGHNxAH4AAz+5HiDTgsyUc3EAfgADP8m7qlOqlapzcQB+AAM/+lTENLqI+nNxAH4AAz/5MsvK7km3c3EAfgADQAZjdWGRe7xzcQB+AAO/4+xO2XGrr3NxAH4AAz/ySyI7EJF9c3EAfgADP/l/vtkMGyZzcQB+AAO//2Z1+bSmnHNxAH4AA7/LVgwLCYxic3EAfgADv/AKR4asTalzcQB+AAM/9UvXsdIFNnNxAH4AAz/mejrhqfC2c3EAfgADP+xJKlQ2BuJzcQB+AAM/3YuaxErEkHNxAH4AAz/1aLkSIg7rc3EAfgADP+CpF5smXg5zcQB+AAO//KZgpZr3P3NxAH4AAz/esRL/++38c3EAfgADv+DYLtBvw8hzcQB+AANAAHPRA7iL2XNxAH4AA7/29wxghdHYc3EAfgADv9dtcp43rrxzcQB+AAM/v2yG49nN0nNxAH4AAz/+uCS7+3cbc3EAfgADwATTGepfF6ZzcQB+AANAAhX6bgPQ/nNxAH4AA7+8XTE+L/oAc3EAfgADP/s3MooLoH5zcQB+AAO/7NQklo8p7HNxAH4AAz/QSfwrAOBec3EAfgADP9nAFJAwWqBzcQB+AANAAILhiWm4ynNxAH4AA7/oLbfbsaFZc3EAfgADv8l+VETYQrBzcQB+AAO/6UUu4AulLnNxAH4AA7+32XJT4HMCc3EAfgADv/kBq7JpC2xzcQB+AAM/y96QXLbSL3NxAH4AA7/yleOMuNucc3EAfgADP/D/0vR0mcpzcQB+AAM/xfZodCuHtHNxAH4AA7/wxYNTJdwac3EAfgADP/ocQnKfpCJzcQB+AAM/4ssGBg3Us3NxAH4AA7/IRlRMa4iDc3EAfgADP/3KTrlmMy9zcQB+AAM/4q8OjO2eFnNxAH4AA0AHxRsQvttOc3EAfgADP98WcRt0vFN4eA==";

  public static void main(String[] args) {
    NamedBuilder nb = NamedBuilder.fromDiscovery();
    Function<String, Object> decoder = (Function<String, Object>) nb.build("f.fromBase64()");
    InvertibleMapper<List<Double>, NumericalDynamicalSystem<?>> iMapper = (InvertibleMapper<List<Double>,
        NumericalDynamicalSystem<?>>) nb.build(
        MAPPER_S);
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], NavigationEnvironment.State> sat =
        (SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], NavigationEnvironment.State>) nb.build(
        SIM_S);
    List<Double> genotype = ((List<List<Double>>) decoder.apply(GENO_S)).getFirst();
    NumericalDynamicalSystem<?> agent = iMapper.mapperFor(sat.example().orElseThrow()).apply(genotype);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome = sat.simulate(
        agent);
    NavigationDrawer drawer = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    drawer.videoBuilder().save(new File("../breaking-walls.mp4"), outcome);
    drawer.show(outcome);
  }
}
