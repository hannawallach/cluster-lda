# cat scriptname.R | R --slave --vanilla --args ...

results_dir_base = commandArgs()[5]
num_runs = commandArgs()[6]

f <- paste(results_dir_base, '1-', num_runs, '_av_num_clusters.txt', sep="")

for (id in 1:num_runs) {
  results_dir <- paste(results_dir_base, id, sep="")
  file <- paste(results_dir, '/num_clusters.txt', sep="")
  data <- read.table(file, header=FALSE)
  if (id == 1) {
     all_data <- data$V1
  }
  else {
     all_data <- c(all_data, data$V1)
  }
}

cat(mean(all_data), sd(all_data), "\n", sep=" ", file=f);
