# cat scriptname.R | R --slave --vanilla --args ...

results_dir_base = commandArgs()[5]
num_runs = commandArgs()[6]

plot_file <- paste(results_dir_base, '1-', num_runs, '_log_probs.pdf', sep="")

pdf(plot_file)

par(lwd=0.5) # set the line width

for (id in 1:num_runs) {
  results_dir <- paste(results_dir_base, id, sep="")
  file <- paste(results_dir, '/log_prob.txt', sep="")
  data <- read.table(file, header=FALSE)
  if (id == 1) {
     plot(data$V1, type="l");
  }
  else {
     lines(data$V1);
  }
}

dev.off()
