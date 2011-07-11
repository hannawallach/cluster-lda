# cat scriptname.R | R --slave --vanilla --args ...

results_dir = commandArgs()[5]

lp_file <- paste(results_dir, '/log_prob_clusters_permutations.txt', sep="")
stats_file <- paste(results_dir, '/permutation_stats.txt', sep="")

data <- read.table(lp_file, header=FALSE)

cat(mean(data$V1), sd(data$V1), '\n', sep=' ', file=stats_file)
