set terminal pdf  background "#ffffff" enhanced font "arial,6" fontscale 1.0
set output 'random-startstop.pdf'
set border 2 front linetype -1 linewidth 1.000
set boxwidth 0.5 absolute
set style fill   solid 1 border lt -1
unset key
set pointsize 0.5
set style data boxplot
set xtics border in scale 0,0 nomirror norotate  offset character 0, 0, 0 autojustify
set xtics  norangelimit
set xtics   ("k=1" 1, "k=2" 2, "k=3" 3, "k=4" 4, "k=5" 5)
set ytics border in scale 1,0.5 nomirror norotate  offset character 0, 0, 0 autojustify
set ytics 0,10
set yrange [ 0.00000:50] noreverse nowriteback
set ylabel "Random-Case: Starts and Stops"

plot 'startstop_random_k1.csv' using (1):1 lc rgb "#ae293e", 'startstop_random_k2.csv' using (2):1 lc rgb "#4c9ab6", 'startstop_random_k3.csv' using (3):1 lc rgb "#FFCD00", 'startstop_random_k4.csv' using (4):1 lc rgb "#da6f00", 'startstop_random_k5.csv' using (5):1 lc rgb "#89A400"

set output 'random-startstop_nooutliers.pdf'
set style boxplot nooutliers
plot 'startstop_random_k1.csv' using (1):1 lc rgb "#ae293e", 'startstop_random_k2.csv' using (2):1 lc rgb "#4c9ab6", 'startstop_random_k3.csv' using (3):1 lc rgb "#FFCD00", 'startstop_random_k4.csv' using (4):1 lc rgb "#da6f00", 'startstop_random_k5.csv' using (5):1 lc rgb "#89A400"
