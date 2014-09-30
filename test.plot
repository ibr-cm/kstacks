set terminal pngcairo  background "#ffffff" enhanced font "arial,10" fontscale 1.0 size 600, 400 
set output 'demoplot.png'
set border 2 front linetype -1 linewidth 1.000
set boxwidth 0.5 absolute
set style fill   solid 0.25 border lt -1
unset key
set pointsize 0.5
set style data boxplot
set xtics border in scale 0,0 nomirror norotate  offset character 0, 0, 0 autojustify
set xtics  norangelimit
set xtics   ("random (k=1)" 1.00000, "random (k=2)" 2.00000, "random (k=3)" 3.00000, "random (k=4)" 4.00000, "random (k=5)" 5.00000)
set ytics border in scale 1,0.5 nomirror norotate  offset character 0, 0, 0 autojustify
set yrange [ 0.00000 : 200.000 ] noreverse nowriteback
plot 'backOrder_1.csv' using (1):1 lc rgb "red", 'backOrder_2.csv' using (2):1 lc rgb "red", 'backOrder_2.csv' using (3):1 lc rgb "red", 'backOrder_2.csv' using (4):1 lc rgb "red", 'backOrder_2.csv' using (5):1 lc rgb "red"
