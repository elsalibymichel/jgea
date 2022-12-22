/**
 * @author "Eric Medvet" on 2022/08/29 for jgea
 */
module io.github.ericmedvet.jgea.sample {
  opens io.github.ericmedvet.jgea.sample.experimenter to io.github.ericmedvet.jnb.core;
  requires io.github.ericmedvet.jgea.core;
  requires io.github.ericmedvet.jgea.problem;
  requires io.github.ericmedvet.jgea.tui;
  requires io.github.ericmedvet.jgea.experimenter;
  requires io.github.ericmedvet.jnb.core;
  requires com.google.common;
  requires java.logging;
  requires java.desktop;
}