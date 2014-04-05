# change ratings to -1 based on how many rating each article has
import csv
import math

doc_dict = {}

with open('../datasets/yow-userstudy/python/yow-docid-occurences.csv', 'rb') as f:
	reader = csv.reader(f)
	next(reader, None) # skip header

	for row in reader:
		# check if doc has an id
		if not row[0] in doc_dict and int(row[1]) > 1:
			occurences = int(row[1])
			a = math.floor(occurences/2)

			if a > 2:
				doc_dict[row[0]] = a 
			else:
				doc_dict[row[0]] = 1
f.close()

with open('../datasets/yow-userstudy/python/yow-data.csv', 'rb') as f:
	reader = csv.reader(f)
	next(reader, None) # skip header
	out_file = open('../datasets/yow-userstudy/python/yow-smart-sample-implicit-2.csv', 'wb')
	writer = csv.writer(out_file, delimiter=',')

	# write header
	writer.writerow(['# user_id','DOC_ID', 'user_like', 'TimeOnPage','TimeOnMouse', 'PageTimesMouse'])

	counter = 0

	# user_id, doc_id, user_like, time_on_page, time_on_mouse
	for row in reader:
		if row[1] in doc_dict and doc_dict[row[1]] > 0:
			row[2] = -1
			doc_dict[row[1]] = doc_dict[row[1]] -1
			counter = counter + 1

		# check for empty strings
		for i in range(2,len(row)):
			if row[i] == '':
				row[i] = 0

		writer.writerow(row) 
f.close()

with open('../datasets/yow-userstudy/python/yow-smart-sample-implicit-2.csv', 'rb') as f:
	reader = csv.reader(f)
	next(reader, None) # skip header
	out_file = open('../datasets/yow-userstudy/python/yow-smart-sample-explicit-2.csv', 'wb')
	writer = csv.writer(out_file, delimiter=',')

	# write header
	writer.writerow(['# user_id','DOC_ID', 'user_like'])

	for row in reader:
		if int(row[2]) < 1:
			continue

		writer.writerow([ row[0], row[1], row[2] ]) 
f.close()

print len(doc_dict)
print counter



