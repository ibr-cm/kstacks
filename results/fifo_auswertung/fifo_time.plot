set terminal pdf  background "#ffffff" enhanced font "arial,6" fontscale 1.0
set output 'fifo-time.pdf'
set border 2 front linetype -1 linewidth 1.000
set boxwidth 0.5 absolute
set style fill   solid 1 border lt -1
unset key
set pointsize 0.5
set style data boxplot
#set style boxplot fraction 0.95
set xtics border in scale 0,0 nomirror norotate  offset character 0, 0, 0 autojustify
set xtics  norangelimit
set xtics   ("k=1" 1, "k=2" 2, "k=3" 3, "k=4" 4, "k=5" 5, "k=6" 6, "k=8" 7, "k=10" 8, "k=12" 9)
set ytics border in scale 1,0.5 nomirror norotate  offset character 0, 0, 0 autojustify
set ytics 0,15
set ylabel "FIFO-Case: Time for leaving the parking lot (minutes)"
set yrange [ 0.00000 : ]
plot 'backOrder_fifo_k1.csv' using (1):($1*0.9/60) lc rgb "#ae293e", 'backOrder_fifo_k2.csv' using (2):($1*0.9/60) lc rgb "#4c9ab6", 'backOrder_fifo_k3.csv' using (3):($1*0.9/60) lc rgb "#FFCD00", 'backOrder_fifo_k4.csv' using (4):($1*0.9/60) lc rgb "#da6f00", 'backOrder_fifo_k5.csv' using (5):($1*0.9/60) lc rgb "#89A400", 'backOrder_fifo_k6.csv' using (6):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k8.csv' using (7):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k10.csv' using (8):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k12.csv' using (9):($1*0.9/60) lc rgb "#914990"

set output 'fifo-time_nooutliers.pdf'
set style boxplot nooutliers
plot 'backOrder_fifo_k1.csv' using (1):($1*0.9/60) lc rgb "#ae293e", 'backOrder_fifo_k2.csv' using (2):($1*0.9/60) lc rgb "#4c9ab6", 'backOrder_fifo_k3.csv' using (3):($1*0.9/60) lc rgb "#FFCD00", 'backOrder_fifo_k4.csv' using (4):($1*0.9/60) lc rgb "#da6f00", 'backOrder_fifo_k5.csv' using (5):($1*0.9/60) lc rgb "#89A400", 'backOrder_fifo_k6.csv' using (6):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k8.csv' using (7):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k10.csv' using (8):($1*0.9/60) lc rgb "#914990", 'backOrder_fifo_k12.csv' using (9):($1*0.9/60) lc rgb "#914990"

