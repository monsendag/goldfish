tic

M = csvread('ratings-synthesized-small.csv');
%X = sptensor;

% M: user id, movie id, rating, read-rating 

numrows = size(M,1);

rating_subs = [M(:,1:2) ones(numrows,1)];
read_rating_subs = [M(:,1:2) 2*ones(numrows,1)];

subs = vertcat(rating_subs, read_rating_subs);

vals = vertcat(M(:,3), M(:,4));

X = sptensor(subs,vals);
toc
%for i=1:size(M,1);
 %   X(M(i,1),M(i,2),1) = M(i,3);
 %   X(M(i,1),M(i,2),2) = M(i,4);
%end
%toc

