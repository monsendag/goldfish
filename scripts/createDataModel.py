# script :: creating a datamodel that fits mahout from ratings.dat

ratings_dat = open('../data/movielens-1m/ratings.dat', 'r')

ratings_csv = open('../data/movielens-1m/ratings_without_timestamp.txt', 'w')

for line in ratings_dat:
	arr = line.split('::')
	new_line = ','.join(arr[:3])+'\n';

	ratings_csv.write(new_line)

ratings_dat.close()
ratings_csv.close()