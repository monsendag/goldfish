#!/usr/bin/env python
# script :: creating a datamodel that fits mahout from ratings.dat



ratings_dat = open('../data/movielens-1m/users.dat', 'r')
ratings_csv = open('../data/movielens-1m/users.txt', 'w')

for line in ratings_dat:
	arr = line.split('::')
	new_line = '\t'.join(arr)

	ratings_csv.write(new_line)

ratings_dat.close()
ratings_csv.close()
