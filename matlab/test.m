% // ==================================
% // test program
% // ==================================
clear

%kiwi = Kiwi('../datasets/movielens-synthesized/ratings-synthesized.csv');
%kiwi.recommend(1, 2)


X = sptenrand([3 3 2],10);
T = tucker_als(X, 2);

F = full(T);
