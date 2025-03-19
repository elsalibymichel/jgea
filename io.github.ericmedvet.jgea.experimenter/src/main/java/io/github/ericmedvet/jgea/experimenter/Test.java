package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.InvertibleMapper;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.Pair;
import io.github.ericmedvet.jsdynsym.control.BiSimulation;
import io.github.ericmedvet.jsdynsym.control.HomogeneousBiAgentTask;
import io.github.ericmedvet.jsdynsym.control.pong.PongAgent;
import io.github.ericmedvet.jsdynsym.control.pong.PongEnvironment;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Test {
    private static final NamedBuilder<Object> BUILDER = NamedBuilder.fromDiscovery();
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String folder = "/home/il_bello/IdeaProjects/results/pong-video/2025-03-14--14-31-22/";
        String resultsCSVPath = folder + "allBest.csv";
        String score1 = "ds.e.pong.score1()";
        String score2 = "ds.e.pong.score2()";
        String numberCollisionsWithBall1 = "ds.e.pong.numberOfCollisionsWithBall1()";
        String numberCollisionsWithBall2 = "ds.e.pong.numberOfCollisionsWithBall2()";
        
        Map<String, Integer> opponentIndices = new HashMap<>();
        List<String> opponentNames = new ArrayList<>();
        List<Pair<String, NumericalDynamicalSystem<?>>> opponents = new ArrayList<>();
        int maxSeed = 0;
        
        try (Reader reader = Files.newBufferedReader(Paths.get(resultsCSVPath))) {
            CSVParser csvParser = CSVFormat.Builder.create().setDelimiter(";").build().parse(reader);
            List<CSVRecord> records = csvParser.getRecords();
            CSVRecord headerCSV = records.getFirst();
            
            int mapperColumnIndex = 0, genotypeColumnIndex = 0, nameColumnIndex = 0, seedColumnIndex = 0;
            for (int i = 0; i < headerCSV.size(); i++) {
                String columnName = headerCSV.get(i);
                if (columnName.contains("mapper")) {
                    mapperColumnIndex = i;
                } else if (columnName.contains("genotype")) {
                    genotypeColumnIndex = i;
                } else if (columnName.contains("name")) {
                    nameColumnIndex = i;
                } else if (columnName.contains("seed")) {
                    seedColumnIndex = i;
                }
            }
            
            Function<String, Object> deserializer = (Function<String, Object>) BUILDER.build("f.fromBase64()");
            PongEnvironment environment = (PongEnvironment) BUILDER.build("ds.e.pong()");
            NumericalDynamicalSystem<?> exampleNDS = environment.exampleAgent();
            
            for (int i = 1; i < records.size(); i++) {
                NumericalDynamicalSystem<?> opponent = ((InvertibleMapper<Object, NumericalDynamicalSystem<?>>) BUILDER.build(records.get(i).get(mapperColumnIndex)))
                        .mapperFor(exampleNDS)
                        .apply(deserializer.apply(records.get(i).get(genotypeColumnIndex)));
                String name = records.get(i).get(nameColumnIndex);
                maxSeed = Math.max(maxSeed, Integer.parseInt(records.get(i).get(seedColumnIndex)));
                if (!opponentIndices.containsKey(name)) {
                    opponentIndices.put(name, opponentNames.size());
                    opponentNames.add(name);
                }
                opponents.add(new Pair<>(name, opponent));
            }
            
            opponentIndices.put("simple", opponentNames.size());
            opponentNames.add("simple");
            for (int i = 0; i < maxSeed; i++) {
                opponents.add(new Pair<>("simple", new PongAgent()));
            }
            
            int numOpponents = opponentNames.size();
            String[][] scoreMatrix = new String[numOpponents][numOpponents];
            String[][] collisionsMatrix = new String[numOpponents][numOpponents];
            int[][] matchCounts = new int[numOpponents][numOpponents];
            double[][] totalScore1 = new double[numOpponents][numOpponents];
            double[][] totalScore2 = new double[numOpponents][numOpponents];
            double[][] totalCollisions1 = new double[numOpponents][numOpponents];
            double[][] totalCollisions2 = new double[numOpponents][numOpponents];
            
            HomogeneousBiAgentTask<NumericalDynamicalSystem<?>, double[], double[], PongEnvironment.State> task =
                    HomogeneousBiAgentTask.fromHomogenousBiEnvironment(() -> environment, s -> false, new DoubleRange(0, 60), 0.05);
            
            for (Pair<String, NumericalDynamicalSystem<?>> opponent1 : opponents) {
                for (Pair<String, NumericalDynamicalSystem<?>> opponent2 : opponents) {
                    int index1 = opponentIndices.get(opponent1.first());
                    int index2 = opponentIndices.get(opponent2.first());
                    
                    BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>> matchOutcome =
                            task.simulate(new Pair<>(opponent1.second(), opponent2.second()));
                    
                    double fitness1 = ((FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(score1)).apply(matchOutcome);
                    double fitness2 = ((FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(score2)).apply(matchOutcome);
                    double collisions1 = ((FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(numberCollisionsWithBall1)).apply(matchOutcome);
                    double collisions2 = ((FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(numberCollisionsWithBall2)).apply(matchOutcome);
                    
                    totalScore1[index1][index2] += fitness1;
                    totalScore2[index1][index2] += fitness2;
                    totalCollisions1[index1][index2] += collisions1;
                    totalCollisions2[index1][index2] += collisions2;
                    matchCounts[index1][index2]++;
                }
            }
            
            for (int i = 0; i < numOpponents; i++) {
                for (int j = 0; j < numOpponents; j++) {
                    if (matchCounts[i][j] > 0) {
                        scoreMatrix[i][j] = " = " + (totalScore1[i][j] / matchCounts[i][j]) + " / " + (totalScore2[i][j] / matchCounts[i][j]);
                        collisionsMatrix[i][j] = " = " + (totalCollisions1[i][j] / matchCounts[i][j]) + " / " + (totalCollisions2[i][j] / matchCounts[i][j]);
                    }
                }
            }
            
            try (BufferedWriter scoreWriter = Files.newBufferedWriter(Paths.get(folder + "scores-formula.csv"));
                 BufferedWriter collisionsWriter = Files.newBufferedWriter(Paths.get(folder + "collisions-formula.csv"))) {
                scoreWriter.write(";" + String.join(";", opponentNames) + "\n");
                collisionsWriter.write(";" + String.join(";", opponentNames) + "\n");
                for (int i = 0; i < numOpponents; i++) {
                    scoreWriter.write(opponentNames.get(i) + ";" + String.join(";", scoreMatrix[i]) + "\n");
                    collisionsWriter.write(opponentNames.get(i) + ";" + String.join(";", collisionsMatrix[i]) + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

/*
public class Test {
    private static final NamedBuilder<Object> BUILDER = NamedBuilder.fromDiscovery();
    
    public static void main(String[] args) {
        String resultsCSVPath = "/home/il_bello/IdeaProjects/results/pong-test/2025-03-07--18-55-46/allBest.csv";
        String problemEnvironment = "ds.e.pong()";
        String score1 = "ds.e.pong.score1()";
        String score2 = "ds.e.pong.score2()";
        String numberCollisionsWithBall1 = "ds.e.pong.numberOfCollisionsWithBall1()";
        String numberCollisionsWithBall2 = "ds.e.pong.numberOfCollisionsWithBall2()";
        try (Reader reader = Files.newBufferedReader(Paths.get(resultsCSVPath))) {
            CSVParser csvParser = CSVFormat.Builder.create().setDelimiter(";").build().parse(reader);
            List<CSVRecord> records = csvParser.getRecords();
            CSVRecord headerCSV = records.getFirst();
            int mCIndex = 0;
            int gCIndex = 0;
            int nCIndex = 0;
            int sCIndex = 0;
            for (int i = 0; i < headerCSV.size(); i++) {
                String columnName = headerCSV.get(i);
                if (columnName.contains("mapper")) {
                    mCIndex = i;
                } else if (columnName.contains("genotype")) {
                    gCIndex = i;
                } else if (columnName.contains("name")) {
                    nCIndex = i;
                } else if (columnName.contains("seed")) {
                    sCIndex = i;
                }
            }
            final int mapperColumnIndex = mCIndex;
            final int genotypeColumnIndex = gCIndex;
            final int nameColumnIndex = nCIndex;
            final int seedColumnIndex = sCIndex;
            @SuppressWarnings("unchecked") Function<String, Object> deserializer = ((Function<String, Object>) BUILDER.build("f.fromBase64()"));
            PongEnvironment environment = (PongEnvironment) BUILDER.build(problemEnvironment);
            NumericalDynamicalSystem<?> exampleNDS = environment.exampleAgent();
            List<Pair<String, NumericalDynamicalSystem<?>>> opponents = new ArrayList<>();
            int maxSeed = 0;
            for (int i = 1; i < records.size(); i++) {
                //noinspection unchecked
                NumericalDynamicalSystem<?> opponent = ((InvertibleMapper<Object, NumericalDynamicalSystem<?>>) BUILDER.build(records.get(i).get(mapperColumnIndex)))
                        .mapperFor(exampleNDS)
                        .apply(deserializer.apply(records.get(i).get(genotypeColumnIndex)));
                String name = records.get(i).get(nameColumnIndex);
                maxSeed = Math.max(maxSeed, Integer.parseInt(records.get(i).get(seedColumnIndex)));
                opponents.addLast(new Pair<>(name, opponent));
                //opponentsList.addLast(opponent);
            }
            //opponentsList.add((NumericalDynamicalSystem<?>) BUILDER.build("ds.opponent.pong.simple()"));
            for (int i = 0; i < maxSeed; i++) {
                opponents.addLast(new Pair<>("simple", new PongAgent()));
            }
            HomogeneousBiAgentTask<NumericalDynamicalSystem<?>, double[], double[], PongEnvironment.State> task =
                    HomogeneousBiAgentTask.fromHomogenousBiEnvironment(() -> environment, s -> false, new DoubleRange(0, 30), 0.05);
            for (Pair<String, NumericalDynamicalSystem<?>> opponent1 : opponents) {
                for (Pair<String, NumericalDynamicalSystem<?>> opponent2 : opponents) {
                    Pair<String, String> matchName = new Pair<>(opponent1.first(), opponent2.first());
                    BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>> matchOutcome =
                            task.simulate(new Pair<>(opponent1.second(), opponent2.second()));
                    @SuppressWarnings("unchecked") double fitness1 = (
                            (FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(score1)
                    ).apply(matchOutcome);
                    @SuppressWarnings("unchecked") double fitness2 = (
                            (FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(score2)
                    ).apply(matchOutcome);
                    @SuppressWarnings("unchecked") double collisions1 = (
                            (FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(numberCollisionsWithBall1)
                    ).apply(matchOutcome);
                    @SuppressWarnings("unchecked") double collisions2 = (
                            (FormattedNamedFunction<BiSimulation.Outcome<HomogeneousBiAgentTask.Step<double[], double[], PongEnvironment.State>>, Double>) BUILDER.build(numberCollisionsWithBall2)
                    ).apply(matchOutcome);
                    System.out.println(fitness1 + "/" + fitness2 + "   " + collisions1 + "/" + collisions2);
                }
            }
            
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        
    }
    
}
*/
