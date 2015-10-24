#! /bin/sh

# 1/orbit
	# same mass
		# 20 asteroids
java Orbits  20 1  250e9  700e9  0 >  ../1.txt
		# 50 asteroids
java Orbits  50 1  250e9  700e9  0 >  ../2.txt
		# 100 asteroids
java Orbits 100 1  250e9  700e9  0 >  ../3.txt
	# 123x mass
		# 20 asteroids
java Orbits  20 1  250e9  700e9  7 >  ../4.txt
		# 50 asteroids
java Orbits  50 1  250e9  700e9 17 >  ../5.txt
		# 100 asteroids
java Orbits 100 1  250e9  700e9 34 >  ../6.txt
# 2/orbit
	# same mass
		# 20 asteroids
java Orbits  10 2  250e9  700e9  0 >  ../7.txt
		# 50 asteroids
java Orbits  25 2  250e9  700e9  0 >  ../8.txt
		# 100 asteroids
java Orbits  50 2  250e9  700e9  0 >  ../9.txt
	# 123x mass
		# 20 asteroids
java Orbits  10 2  250e9  700e9  7 >  ../10.txt
		# 50 asteroids
java Orbits  25 2  250e9  700e9 17 >  ../11.txt
		# 100 asteroids
java Orbits  50 2  250e9  700e9 34 >  ../12.txt
# 3-bands
	# same mass
		# 20 asteroids
java Orbits   8 1  250e9  350e9  0 >  ../13.txt
java Orbits   4 1  650e9  700e9  0 >> ../13.txt
java Orbits   8 1 1000e9 1100e9  0 >> ../13.txt
		# 50 asteroids
java Orbits  20 1  250e9  350e9  0 >  ../14.txt
java Orbits  10 1  650e9  700e9  0 >> ../14.txt
java Orbits  20 1 1000e9 1100e9  0 >> ../14.txt
		# 100 asteroids
java Orbits  40 1  250e9  350e9  0 >  ../15.txt
java Orbits  20 1  650e9  700e9  0 >> ../15.txt
java Orbits  40 1 1000e9 1100e9  0 >> ../15.txt
	# 123x mass
		# 20 asteroids
java Orbits   8 1  250e9  350e9  3 >  ../16.txt
java Orbits   4 1  650e9  700e9  1 >> ../16.txt
java Orbits   8 1 1000e9 1100e9  3 >> ../16.txt
		# 50 asteroids
java Orbits  20 1  250e9  350e9  7 >  ../17.txt
java Orbits  10 1  650e9  700e9  4 >> ../17.txt
java Orbits  20 1 1000e9 1100e9  7 >> ../17.txt
		# 100 asteroids
java Orbits  40 1  250e9  350e9 14 >  ../18.txt
java Orbits  20 1  650e9  700e9  7 >> ../18.txt
java Orbits  40 1 1000e9 1100e9 14 >> ../18.txt
