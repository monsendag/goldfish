function x = rmse(initial_rating, computed_ratings)
sum = 0;

[sub, values] = find(initial_rating);

matrix_size = size(sub);

for i = 1:matrix_size(1)
    if sub(i,3,1) ~= 2
        sum = sum + (values(i) - computed_ratings(sub(i,1,1), sub(i,2,1) ...
            ,sub(i,3,1)))^2;
    end
end

x = sqrt(sum/matrix_size(1));

end

