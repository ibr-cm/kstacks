set terminal pdf  background "#ffffff" enhanced font "arial,6" fontscale 1.0
set output 'lifo-startstop_triple.pdf'
set border 2 front linetype -1 linewidth 1.000
set boxwidth 0.5 absolute
set style fill   solid 1 border lt -1
unset key
set pointsize 0.5
set style data boxplot
set xtics border in scale 0,0 nomirror norotate  offset character 0, 0, 0 autojustify
set xtics  norangelimit
set xtics   ("k=1" 1, "k=2" 2, "k=3" 3, "k=4" 4, "k=5" 5, "k=6" 6, "k=8" 7, "k=10" 8, "k=12" 9)
set ytics border in scale 1,0.5 nomirror norotate  offset character 0, 0, 0 autojustify
set ytics 0,2
set yrange [ 0.00000 : 4]
set ylabel "LIFO-Case: Starts and Stops"
plot 'startstop_lifo_k1.csv' using (1):(($1)/2) lc rgb "#ae293e", 'startstop_lifo_k2.csv' using (2):(($1)/2) lc rgb "#4c9ab6", 'startstop_lifo_k3.csv' using (3):(($1)/2) lc rgb "#FFCD00", 'startstop_lifo_k4.csv' using (4):(($1)/2) lc rgb "#da6f00", 'startstop_lifo_k5.csv' using (5):(($1)/2) lc rgb "#89A400", 'startstop_lifo_k6.csv' using (6):(($1)/2) lc rgb "#914990", 'startstop_lifo_k8.csv' using (7):(($1)/2) lc rgb "#914990", 'startstop_lifo_k10.csv' using (8):(($1)/2) lc rgb "#914990", 'startstop_lifo_k12.csv' using (9):(($1)/2) lc rgb "#914990"


set output 'lifo-startstop_nooutliers_triple.pdf'
set style boxplot nooutliers
plot 'startstop_lifo_k1.csv' using (1):(($1)/2) lc rgb "#ae293e", 'startstop_lifo_k2.csv' using (2):(($1)/2) lc rgb "#4c9ab6", 'startstop_lifo_k3.csv' using (3):(($1)/2) lc rgb "#FFCD00", 'startstop_lifo_k4.csv' using (4):(($1)/2) lc rgb "#da6f00", 'startstop_lifo_k5.csv' using (5):(($1)/2) lc rgb "#89A400", 'startstop_lifo_k6.csv' using (6):(($1)/2) lc rgb "#914990", 'startstop_lifo_k8.csv' using (7):(($1)/2) lc rgb "#914990", 'startstop_lifo_k10.csv' using (8):(($1)/2) lc rgb "#914990", 'startstop_lifo_k12.csv' using (9):(($1)/2) lc rgb "#914990"