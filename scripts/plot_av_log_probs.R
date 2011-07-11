# cat scriptname.R | R --slave --vanilla --args ...

results_dir_base = commandArgs()[5]
num_runs = commandArgs()[6]

file <- paste(results_dir_base, '1-', num_runs, '_av_log_probs.pdf', sep="")

pdf(file)

par(lwd=0.5) # set the line width

for (id in 1:num_runs) {
  results_dir <- paste(results_dir_base, id, sep="")
  file <- paste(results_dir, '/log_prob.txt', sep="")
  data <- read.table(file, header=FALSE)
  if (id == 1) {
     all_data <- data$V1
  }
  else {
     all_data <- all_data + data$V1
  }
}

all_data <- all_data / as.numeric(num_runs)

plot(all_data, type="l");

dev.off()
