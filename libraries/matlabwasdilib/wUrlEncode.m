%courtesy of https://titanwolf.org/Network/Articles/Article?AID=c16b0563-60f6-46e2-9c50-092039fa86bc#gsc.tab=0
function u = wUrlEncode(s)
	u = '';
	for k = 1:length(s),
		if isalnum(s(k))
			u(end+1) = s(k);
		else
			u=[u,'%',dec2hex(s(k)+0)];
		end; 	
	end
end