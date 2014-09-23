set terminal pngcairo  background "#ffffff" enhanced font "arial,10" fontscale 1.0 size 600, 400 
set output 'startstop.png'
set border 2 front linetype -1 linewidth 1.000
set boxwidth 0.5 absolute
set style fill   solid 0.25 border lt -1
unset key
set pointsize 0.5
set style data boxplot
set xtics border in scale 0,0 nomirror norotate  offset character 0, 0, 0 autojustify
set xtics  norangelimit
set xtics   ("k=1" 1.00000, "k=2" 2.00000, "k=3" 3.00000)
set ytics border in scale 1,0.5 nomirror norotate  offset character 0, 0, 0 autojustify
set yrange [ 0.00000 : 10.000 ] noreverse nowriteback
plot 'startstop_random_k1.csv' using (1):($1*2), 'startstop_random_k2.csv' using (2):($1*2), 'startstop_random_k3.csv' using (3):($1*2)